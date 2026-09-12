export const optionalMoney = (value) => value == null || value === '' || !Number.isFinite(Number(value)) ? null : Number(value);
// Order's legacy primitive cost fields contain 0 even without provider financials.
// Only AVAILABLE makes those provider amounts known; it must not infer customer payment.
export const orderFinancials = (detail = {}) => {
  const available = detail.financialDataStatus === 'AVAILABLE';
  return { totalAmount: optionalMoney(detail.totalAmount),
    commissionAmount: available ? optionalMoney(detail.commissionAmount) : null,
    deliveryFeeAmount: available ? optionalMoney(detail.deliveryFee) : null,
    couponAmount: available ? optionalMoney(detail.couponCost) : null,
    platformSupportAmount: available ? optionalMoney(detail.platformSupportAmount) : null,
    menuCostAmount: optionalMoney(detail.totalMenuCost), packagingAmount: optionalMoney(detail.totalPackagingFee),
    netProfit: optionalMoney(detail.netProfit), financialDataStatus: detail.financialDataStatus || 'UNAVAILABLE',
  };
};
