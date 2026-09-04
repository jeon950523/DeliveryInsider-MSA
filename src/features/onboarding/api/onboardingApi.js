import httpClient from '../../../shared/api/httpClient.js';

export const verifyBusiness = (payload) =>
  httpClient.post(
    '/api/stores/business-verifications',
    payload
  );

export const createStore = (payload) =>
  httpClient.post(
    '/api/stores',
    payload
  );
