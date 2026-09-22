import axios from 'axios';

const simulatorApi = axios.create({
  baseURL: import.meta.env.VITE_EXTERNAL_API_BASE_URL || 'http://localhost:8101',
  timeout: 7000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const fetchControlStatus = async () => {
  const response = await simulatorApi.get('/api/control/status');
  return response.data;
};

export const fetchExternalStores = async (platformType) => {
  const response = await simulatorApi.get(
    `/api/catalog/providers/${platformType}/stores`,
  );
  return response.data;
};

export const createExternalStore = async (platformType, payload) => {
  const response = await simulatorApi.post(
    `/api/control/providers/${platformType}/stores`,
    payload,
  );
  return response.data;
};

export const fetchExternalMenus = async (platformType, externalStoreId) => {
  const response = await simulatorApi.get(
    `/api/catalog/providers/${platformType}/stores/${externalStoreId}/menus`,
  );
  return response.data;
};

export const createExternalMenu = async (platformType, externalStoreId, payload) => {
  const response = await simulatorApi.post(
    `/api/control/providers/${platformType}/stores/${externalStoreId}/menus`,
    payload,
  );
  return response.data;
};

const financialBase = (platformType, externalStoreId) => (
  `/api/control/providers/${platformType}/stores/${externalStoreId}`
);

export const fetchFeePolicy = async (platformType, externalStoreId) => {
  const response = await simulatorApi.get(`${financialBase(platformType, externalStoreId)}/fee-policy`);
  return response.data;
};

export const saveFeePolicy = async (platformType, externalStoreId, payload) => {
  const response = await simulatorApi.put(`${financialBase(platformType, externalStoreId)}/fee-policy`, payload);
  return response.data;
};

export const fetchCoupons = async (platformType, externalStoreId) => {
  const response = await simulatorApi.get(`${financialBase(platformType, externalStoreId)}/coupons`);
  return response.data;
};

export const createCoupon = async (platformType, externalStoreId, payload) => {
  const response = await simulatorApi.post(`${financialBase(platformType, externalStoreId)}/coupons`, payload);
  return response.data;
};

export const fetchAdSpend = async (platformType, externalStoreId) => {
  const response = await simulatorApi.get(`${financialBase(platformType, externalStoreId)}/ad-spend`);
  return response.data;
};

export const createAdSpend = async (platformType, externalStoreId, payload) => {
  const response = await simulatorApi.post(`${financialBase(platformType, externalStoreId)}/ad-spend`, payload);
  return response.data;
};

export const fetchRecentOrders = async (platformType, externalStoreId, limit = 20) => {
  const response = await simulatorApi.get(
    `/api/control/providers/${platformType}/orders`,
    { params: { externalStoreId, limit } },
  );
  return response.data;
};

export const createExternalOrder = async (platformType, payload) => {
  const response = await simulatorApi.post(
    `/api/control/providers/${platformType}/orders`,
    payload,
  );
  return response.data;
};

export const changeExternalOrderStatus = async (
  platformType,
  externalOrderId,
  payload,
) => {
  const response = await simulatorApi.post(
    `/api/control/providers/${platformType}/orders/${externalOrderId}/status`,
    payload,
  );
  return response.data;
};

export const refundExternalOrder = async (platformType, externalOrderId, payload) => {
  const response = await simulatorApi.post(
    `/api/control/providers/${platformType}/orders/${externalOrderId}/refunds`,
    payload,
  );
  return response.data;
};
