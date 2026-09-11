export const resolveOnboardingTarget = ({
  hasStore,
}) => {
  if (!hasStore) {
    return 'store-onboarding';
  }

  return 'dashboard';
};
