export const premiumFeatureCodes = Object.freeze({
  AI_REPORT_INSIGHT: 'AI_REPORT_INSIGHT',
  REPORT_EXPORT: 'REPORT_EXPORT',
});

export const normalizePremiumFeatures = (response) => {
  const items = Array.isArray(response) ? response : [];

  return items.reduce((features, item) => {
    if (
      Object.values(premiumFeatureCodes).includes(item?.featureCode)
      && typeof item?.entitled === 'boolean'
    ) {
      features[item.featureCode] = item.entitled;
    }

    return features;
  }, {});
};

export const hasPremiumFeature = (features, featureCode) =>
  features?.[featureCode] === true;
