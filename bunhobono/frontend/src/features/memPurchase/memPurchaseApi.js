import api from "@/shared/api/apiClient";

export const createMemPurchaseOrder = (purchaseQuantity) => {
  return api.post("/mem-purchase", { purchaseQuantity });
};

export const confirmMemPurchasePayment = (paymentKey, orderId, amount) => {
  return api.post("/mem-purchase/confirm", {
    paymentKey,
    orderId,
    amount
  });
};
