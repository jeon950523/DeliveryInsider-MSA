import httpClient from '../../../shared/api/httpClient.js';

export const fetchReportOrders = (params = {}) =>
  httpClient.get('/api/reports/orders', { params });

export const fetchReportHistory = (params = {}) =>
  httpClient.get('/api/reports/history', { params });

export const fetchReportSummary = (params = {}) =>
  httpClient.get('/api/reports/summary', { params });

export const fetchReportDailyTrend = (params = {}) =>
  httpClient.get('/api/reports/daily', { params });

export const fetchReportPlatformMetrics = (params = {}) =>
  httpClient.get('/api/reports/platforms/metrics', { params });

export const fetchEstimatedMenuProfit = (params = {}) =>
  httpClient.get('/api/reports/menus/estimated-profit', { params });

export const fetchReportProcessingTimes = (params = {}) =>
  httpClient.get('/api/reports/processing-times', { params });

export const downloadReportXlsx = (params = {}, config = {}) =>
  httpClient.get('/api/reports/export.xlsx', {
    ...config,
    params,
    responseType: 'blob',
  });
