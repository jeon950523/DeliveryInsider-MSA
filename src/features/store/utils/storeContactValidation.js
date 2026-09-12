export const STORE_PHONE_PATTERN =
  /^(?:0\d{1,3}-?\d{3,4}-?\d{4}|1\d{3}-?\d{4})$/;

export const isValidStorePhone = (value) => {
  const phone = String(value ?? '').trim();

  if (!phone) {
    return true;
  }

  return STORE_PHONE_PATTERN.test(phone);
};
