export const internalPaymentDemoEnabled =
  import.meta.env.DEV || import.meta.env.VITE_JONPC_INTERNAL_DEMO === "true";

export const bankTransferEnabled =
  import.meta.env.DEV || import.meta.env.VITE_JONPC_BANK_TRANSFER === "true";

export const stripeEnabled =
  import.meta.env.DEV || import.meta.env.VITE_JONPC_STRIPE === "true";

export const checkoutEnabled = stripeEnabled || bankTransferEnabled || internalPaymentDemoEnabled;
