"use strict";

const { createHash } = require("node:crypto");
const { isIP } = require("node:net");
const { getApp, initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { plans, readCallback, paymentUrl, paymentOutcome } = require("./vnpay");

initializeApp();
const db = getFirestore();
const terminal = defineSecret("VNPAY_TMN_CODE");
const hashSecret = defineSecret("VNPAY_HASH_SECRET");
const region = "asia-southeast1";
const orderLifetimeMs = 15 * 60 * 1000;
const refPattern = /^[a-f0-9]{40}$/;
const requestIdPattern = /^[0-9a-f-]{36}$/i;
const orders = db.collection("premiumOrders");
const profiles = db.collection("users");
const sessions = db.collection("premiumCheckoutSessions");

function checkoutLink(req, order, ref, merchant, secret) {
  const projectId = getApp().options.projectId || process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;
  if (!projectId) throw new Error("Missing Firebase project ID");
  const returnUrl = `https://${region}-${projectId}.cloudfunctions.net/premiumPayments/return`;
  const ip = order.ip ?? (isIP(req.ip ?? "") ? req.ip : "127.0.0.1");
  return paymentUrl({ merchant, secret, ref, order, ip, returnUrl });
}

function ipnReply(res, code, message) {
  return res.json({ RspCode: code, Message: message });
}

async function handleIpn(req, res, merchant, secret) {
  if (req.method !== "GET") return res.sendStatus(405);
  const params = readCallback(req.originalUrl, merchant, secret);
  if (!params) return ipnReply(res, "97", "Invalid signature");
  const ref = params.vnp_TxnRef;
  if (!refPattern.test(ref ?? "")) return ipnReply(res, "01", "Order not found");

  const orderRef = orders.doc(ref);
  const [code, message] = await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(orderRef);
    const order = snapshot.data();
    const outcome = paymentOutcome(order, params);
    if (outcome.code) return [outcome.code, outcome.message];
    const now = Date.now();
    if (outcome.status === "paid") {
      const profileRef = profiles.doc(order.uid);
      const profile = await tx.get(profileRef);
      if (!profile.exists) return ["99", "Profile unavailable"];
      const priorExpiry = Number(profile.get("premiumUntil")) || 0;
      const premiumUntil = Math.max(now, priorExpiry) + order.days * 24 * 60 * 60 * 1000;
      tx.update(profileRef, { premiumUntil });
    }
    tx.update(orderRef, {
      status: outcome.status,
      responseCode: params.vnp_ResponseCode,
      transactionStatus: params.vnp_TransactionStatus,
      resolvedAt: now,
      transactionNo: params.vnp_TransactionNo ?? null
    });
    return ["00", "Confirm success"];
  });
  return ipnReply(res, code, message);
}

async function authenticatedUid(req) {
  const match = /^Bearer (\S+)$/.exec(req.get("Authorization") ?? "");
  if (!match) return null;
  try {
    const token = await getAuth().verifyIdToken(match[1], true);
    return token.uid;
  } catch {
    return null;
  }
}

async function createCheckout(req, res, uid, merchant, secret) {
  const { plan, requestId, expectedAmount } = req.body ?? {};
  if (typeof plan !== "string" || !Object.hasOwn(plans, plan) || !requestIdPattern.test(requestId ?? "")) {
    return res.status(400).json({ error: "INVALID_REQUEST" });
  }
  const { amount, days } = plans[plan];
  if (expectedAmount !== amount) return res.status(409).json({ error: "PRICE_CHANGED" });
  const ref = createHash("sha256").update(`${uid}:${requestId}`).digest("hex").slice(0, 40);
  const orderRef = orders.doc(ref);
  const sessionRef = sessions.doc(uid);
  const now = Date.now();
  const result = await db.runTransaction(async (tx) => {
    const profile = await tx.get(profiles.doc(uid));
    if (!profile.exists) return { error: "PROFILE_REQUIRED" };
    const existing = await tx.get(orderRef);
    if (existing.exists) return { order: existing.data() };
    const session = await tx.get(sessionRef);
    const lastRef = session.get("orderId");
    if (lastRef && lastRef !== ref) {
      const lastOrder = (await tx.get(orders.doc(lastRef))).data();
      if (lastOrder?.status === "pending" && lastOrder.expiresAt > now) {
        return { error: "ORDER_PENDING" };
      }
    }
    const ip = isIP(req.ip ?? "") ? req.ip : "127.0.0.1";
    const order = { uid, plan, amount, days, ip, status: "pending", createdAt: now, expiresAt: now + orderLifetimeMs };
    tx.create(orderRef, order);
    tx.set(sessionRef, { orderId: ref });
    return { order };
  });
  if (result.error) return res.status(409).json({ error: result.error });
  const order = result.order;
  if (order.plan !== plan || order.status !== "pending" || order.expiresAt <= now) {
    return res.status(409).json({ error: "ORDER_CLOSED" });
  }
  return res.json({ orderId: ref, paymentUrl: checkoutLink(req, order, ref, merchant, secret) });
}

async function readStatus(req, res, uid, merchant, secret) {
  const requested = req.query.orderId;
  if (requested !== undefined && (typeof requested !== "string" || !refPattern.test(requested))) {
    return res.status(400).json({ error: "INVALID_REQUEST" });
  }
  const profile = await profiles.doc(uid).get();
  const ref = requested ?? (await sessions.doc(uid).get()).get("orderId");
  const order = ref ? (await orders.doc(ref).get()).data() : null;
  if (requested && (!order || order.uid !== uid)) return res.sendStatus(404);
  const ownOrder = order?.uid === uid ? order : null;
  const status = ownOrder?.status === "pending" && ownOrder.expiresAt <= Date.now()
    ? "expired" : ownOrder?.status ?? "none";
  return res.json({
    status,
    orderId: ownOrder ? ref : null,
    plan: ownOrder?.plan ?? null,
    premiumUntil: Number(profile.get("premiumUntil")) || 0,
    paymentUrl: status === "pending" ? checkoutLink(req, ownOrder, ref, merchant, secret) : null
  });
}

exports.premiumPayments = onRequest(
  { region, invoker: "public", secrets: [terminal, hashSecret] },
  async (req, res) => {
    res.set("Cache-Control", "no-store");
    try {
      const merchant = terminal.value().trim();
      const secret = hashSecret.value();
      if (req.path === "/ipn") return await handleIpn(req, res, merchant, secret);
      if (req.path === "/return" && req.method === "GET") {
        return res.type("html").send(
          '<!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>DogLens Premium</title>' +
          "<main><h1>DogLens Premium</h1><p>Hãy quay lại ứng dụng và bấm Kiểm tra giao dịch để xem kết quả.</p></main></html>"
        );
      }
      if ((req.path !== "/checkout" || req.method !== "POST") &&
          (req.path !== "/status" || req.method !== "GET")) return res.sendStatus(404);
      const uid = await authenticatedUid(req);
      if (!uid) return res.sendStatus(401);
      if (req.path === "/checkout") return await createCheckout(req, res, uid, merchant, secret);
      return await readStatus(req, res, uid, merchant, secret);
    } catch (error) {
      console.error("Premium payment operation failed", error.code ?? error.name);
      if (req.path === "/ipn") return ipnReply(res, "99", "Internal error");
      return res.status(500).json({ error: "SERVER_ERROR" });
    }
  }
);
