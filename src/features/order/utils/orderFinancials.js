export const optionalMoney = (value) => value == null || value === '' || !Number.isFinite(Number(value)) ? null : Number(value);
// Provider costs are shown only when the immutable order snapshot says that the
// provider calculation was completed. PROVISIONAL is still a real calculated
// snapshot; it is not the same as an unavailable provider response.
export const orderFinancials = (detail = {}) => {
  const hasCalculatedProviderFinancials = ['AVAILABLE', 'PROVISIONAL']
    .includes(detail.financialDataStatus);
  return { totalAmount: optionalMoney(detail.totalAmount),
    commissionAmount: hasCalculatedProviderFinancials ? optionalMoney(detail.commissionAmount) : null,
    deliveryFeeAmount: hasCalculatedProviderFinancials ? optionalMoney(detail.deliveryFee) : null,
    couponAmount: hasCalculatedProviderFinancials ? optionalMoney(detail.couponCost) : null,
    platformSupportAmount: hasCalculatedProviderFinancials ? optionalMoney(detail.platformSupportAmount) : null,
    menuCostAmount: optionalMoney(detail.totalMenuCost), packagingAmount: optionalMoney(detail.totalPackagingFee),
    netProfit: optionalMoney(detail.netProfit), financialDataStatus: detail.financialDataStatus || 'UNAVAILABLE',
  };
};
