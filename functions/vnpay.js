"use strict";

const { createHmac, timingSafeEqual } = require("node:crypto");

const checkoutPage = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
const plans = Object.freeze({
  month: { amount: 49000, days: 30 },
  year: { amount: 399000, days: 365 }
});

function canonicalQuery(params) {
  const encode = (value) => encodeURIComponent(value).replace(/%20/g, "+");
  return Object.keys(params).sort()
    .map((key) => `${encode(key)}=${encode(params[key])}`).join("&");
}

function sign(query, secret) {
  return createHmac("sha512", secret).update(query, "utf8").digest("hex");
}

function readCallback(url, merchant, secret) {
  const fields = new URL(url, "https://localhost").searchParams;
  const params = Object.create(null);
  for (const [key, value] of fields) {
    if (!key.startsWith("vnp_")) continue;
    if (Object.hasOwn(params, key)) return null;
    params[key] = value;
  }
  const signature = params.vnp_SecureHash;
  delete params.vnp_SecureHash;
  delete params.vnp_SecureHashType;
  if (!/^[a-f0-9]{128}$/i.test(signature ?? "") || params.vnp_TmnCode !== merchant) return null;
  const expected = sign(canonicalQuery(params), secret);
  return timingSafeEqual(Buffer.from(signature, "hex"), Buffer.from(expected, "hex")) ? params : null;
}

function vnpDate(epoch) {
  return new Date(epoch + 7 * 3600000).toISOString().slice(0, 19).replace(/\D/g, "");
}

function paymentUrl({ merchant, secret, ref, order, ip, returnUrl }) {
  const query = canonicalQuery({
    vnp_Version: "2.1.0",
    vnp_Command: "pay",
    vnp_TmnCode: merchant,
    vnp_Amount: order.amount * 100,
    vnp_CurrCode: "VND",
    vnp_TxnRef: ref,
    vnp_OrderInfo: `DogLens Premium ${ref}`,
    vnp_OrderType: "other",
    vnp_Locale: "vn",
    vnp_ReturnUrl: returnUrl,
    vnp_IpAddr: ip,
    vnp_CreateDate: vnpDate(order.createdAt),
    vnp_ExpireDate: vnpDate(order.expiresAt)
  });
  return `${checkoutPage}?${query}&vnp_SecureHash=${sign(query, secret)}`;
}

function paymentOutcome(order, params) {
  if (!order) return { code: "01", message: "Order not found" };
  if (params.vnp_Amount !== String(order.amount * 100)) return { code: "04", message: "Invalid amount" };
  if (order.status !== "pending") return { code: "02", message: "Order already confirmed" };
  const response = params.vnp_ResponseCode;
  const transactionStatus = params.vnp_TransactionStatus;
  if (!/^\d{2}$/.test(response ?? "") || !/^\d{2}$/.test(transactionStatus ?? "")) {
    return { code: "99", message: "Invalid transaction status" };
  }
  if (response === "00" && transactionStatus === "00") {
    if (!/^\d+$/.test(params.vnp_TransactionNo ?? "")) return { code: "99", message: "Missing transaction reference" };
    return { status: "paid" };
  }
  return { status: response === "07" ? "review" : "failed" };
}

module.exports = { plans, canonicalQuery, sign, readCallback, paymentUrl, paymentOutcome };
