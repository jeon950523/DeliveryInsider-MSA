import httpClient from '../../../shared/api/httpClient.js';

export const fetchBillingPlans = () =>
  httpClient.get('/api/billing/plans');

export const fetchCurrentSubscription = () =>
  httpClient.get('/api/billing/subscription');

export const createSubscription = (planCode) =>
  httpClient.post(
    '/api/billing/subscription',
    { planCode }
  );

export const cancelSubscription = () =>
  httpClient.post(
    '/api/billing/subscription/cancel'
  );

export const prepareTossPayment = () =>
  httpClient.post(
    '/api/billing/payments/toss/prepare'
  );

export const confirmTossPayment = (payload) =>
  httpClient.post(
    '/api/billing/payments/toss/confirm',
    payload
  );

export const failTossPayment = (payload) =>
  httpClient.post(
    '/api/billing/payments/toss/fail',
    payload
  );
