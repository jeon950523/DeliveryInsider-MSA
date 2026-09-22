import httpClient from '../../../shared/api/httpClient.js';

export const issueWebSocketTicket = async () => (
  await httpClient.post('/api/notifications/ws-ticket', undefined, {
    skipServerErrorRedirect: true,
  })
).data;
export const notificationGatewayUrl = () => new URL(httpClient.defaults.baseURL, window.location.origin).href;
