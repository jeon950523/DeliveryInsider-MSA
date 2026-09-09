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

export const fetchExternalMenus = async (platformType, externalStoreId) => {
  const response = await simulatorApi.get(
    `/api/catalog/providers/${platformType}/stores/${externalStoreId}/menus`,
  );
  return response.data;
};

export const fetchRecentOrders = async (platformType, limit = 20) => {
  const response = await simulatorApi.get(
    `/api/control/providers/${platformType}/orders`,
    { params: { limit } },
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
