"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { canonicalQuery, sign, paymentUrl, readCallback, paymentOutcome } = require("./vnpay");

const merchant = "DEMO";
const secret = "unit-test-secret";

test("checkout uses HMAC SHA-512, Việt Nam timestamps and VNPAY scaled amounts", () => {
  const url = new URL(paymentUrl({
    merchant,
    secret,
    ref: "checkout01",
    order: { amount: 49000, createdAt: Date.UTC(2026, 0, 2, 5), expiresAt: Date.UTC(2026, 0, 2, 5, 15) },
    ip: "127.0.0.1",
    returnUrl: "https://example.test/return"
  }));
  assert.equal(url.hostname, "sandbox.vnpayment.vn");
  assert.equal(url.searchParams.get("vnp_Amount"), "4900000");
  assert.equal(url.searchParams.get("vnp_CreateDate"), "20260102120000");
  assert.equal(url.searchParams.get("vnp_ExpireDate"), "20260102121500");
  assert.equal(readCallback(url, merchant, secret).vnp_TxnRef, "checkout01");
});

test("callback signatures reject tampering, duplicate fields and wrong terminal", () => {
  const query = canonicalQuery({ vnp_TmnCode: merchant, vnp_Amount: "4900000", vnp_OrderInfo: "DogLens Premium" });
  assert.equal(query, "vnp_Amount=4900000&vnp_OrderInfo=DogLens+Premium&vnp_TmnCode=DEMO");
  const callback = `/ipn?${query}&vnp_SecureHash=${sign(query, secret)}`;
  assert.equal(readCallback(callback, merchant, secret).vnp_Amount, "4900000");
  assert.equal(readCallback(callback.replace("4900000", "9000000"), merchant, secret), null);
  assert.equal(readCallback(callback + "&vnp_Amount=4900000", merchant, secret), null);
  assert.equal(readCallback(callback, "OTHER", secret), null);
});

test("only a new, matching and successful VNPAY payment grants entitlement", () => {
  const order = { status: "pending", amount: 49000 };
  const params = { vnp_Amount: "4900000", vnp_ResponseCode: "00", vnp_TransactionStatus: "00", vnp_TransactionNo: "123" };
  assert.equal(paymentOutcome(order, params).status, "paid");
  assert.equal(paymentOutcome(order, { ...params, vnp_Amount: "1" }).code, "04");
  assert.equal(paymentOutcome({ ...order, status: "paid" }, params).code, "02");
  assert.equal(paymentOutcome(order, { ...params, vnp_TransactionStatus: "02" }).status, "failed");
  assert.equal(paymentOutcome(order, { ...params, vnp_ResponseCode: "07" }).status, "review");
});
