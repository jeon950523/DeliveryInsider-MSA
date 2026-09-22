import httpClient from '../../../shared/api/httpClient.js';

const managedErrorConfig = {
  skipServerErrorRedirect: true,
};

export const fetchOperationSummary = () => httpClient.get(
  '/api/orders/operation-summary',
  managedErrorConfig,
);
export const fetchTodayOrders = () => httpClient.get(
  '/api/orders/today',
  managedErrorConfig,
);
