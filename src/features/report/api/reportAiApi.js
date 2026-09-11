import httpClient from '../../../shared/api/httpClient.js';

export const requestReportAiInsight = (payload) =>
  httpClient.post(
    '/api/reports/ai-insights',
    payload,
    { skipGlobalErrorRedirect: true }
  );
