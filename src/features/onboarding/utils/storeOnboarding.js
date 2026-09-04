export const STORE_NOT_FOUND_CODE =
  'STORE-001';

export const isStoreNotFoundError = (
  error
) => {
  return (
    error?.response?.status === 404
    && error?.response?.data?.code
      === STORE_NOT_FOUND_CODE
  );
};
