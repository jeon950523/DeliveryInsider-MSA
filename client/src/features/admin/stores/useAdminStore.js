import { defineStore } from 'pinia';
import { reactive } from 'vue';
import * as api from '../api/adminApi.js';

const createResource = () => ({ loading: false, error: '', data: null });

export const useAdminStore = defineStore('admin', () => {
  const resources = reactive({
    userSummary: createResource(), users: createResource(),
    storeSummary: createResource(), stores: createResource(),
    platformSummary: createResource(), connections: createResource(), incidents: createResource(),
    orderSummary: createResource(), subscriptionSummary: createResource(), subscriptions: createResource(),
  });

  const load = async (name, request) => {
    const target = resources[name];
    target.loading = true;
    target.error = '';
    try {
      const response = await request();
      target.data = response.data.data;
      return target.data;
    } catch (error) {
      target.data = null;
      target.error = error.response?.data?.message || '운영 정보를 불러오지 못했습니다.';
      throw error;
    } finally {
      target.loading = false;
    }
  };

  const loadDashboard = () => Promise.allSettled([
    load('userSummary', api.fetchAdminUserSummary),
    load('storeSummary', api.fetchAdminStoreSummary),
    load('platformSummary', api.fetchAdminPlatformSummary),
    load('orderSummary', api.fetchAdminOrderSummary),
    load('subscriptionSummary', api.fetchAdminSubscriptionSummary),
    load('incidents', () => api.fetchAdminIncidents(0, 5)),
    load('connections', () => api.fetchAdminConnections(0, 5)),
    load('subscriptions', () => api.fetchAdminSubscriptions(0, 5)),
  ]);

  return {
    resources,
    loadDashboard,
    loadUsers: (page = 0) => load('users', () => api.fetchAdminUsers(page)),
    loadStores: (page = 0) => load('stores', () => api.fetchAdminStores(page)),
    loadConnections: (page = 0) => load('connections', () => api.fetchAdminConnections(page)),
    loadIncidents: (page = 0) => load('incidents', () => api.fetchAdminIncidents(page)),
    loadSubscriptions: (page = 0) => load('subscriptions', () => api.fetchAdminSubscriptions(page)),
  };
});
