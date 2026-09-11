import { defineStore } from 'pinia';
import { ref } from 'vue';
import { Client } from '@stomp/stompjs';
import { issueWebSocketTicket, notificationGatewayUrl } from '../api/notificationApi.js';
import { createOrderConnection } from '../utils/orderConnection.js';

export const useOrderRealtimeStore = defineStore('order-realtime', () => {
  const storeId = ref(null);
  const status = ref('idle');
  const revision = ref(0);
  const lastSignal = ref(null);
  let connection;
  const stop = () => {
    connection?.stop(); connection = undefined;
    storeId.value = null; status.value = 'idle'; lastSignal.value = null;
  };
  const start = (id) => {
    if (storeId.value === id && connection) return;
    stop();
    if (!Number.isSafeInteger(id) || id <= 0) return;
    storeId.value = id;
    connection = createOrderConnection({ storeId: id, gateway: notificationGatewayUrl(), issueTicket: issueWebSocketTicket,
      makeClient: (config) => new Client(config), onStatus: (value) => { status.value = value; },
      onSignal: (signal) => { lastSignal.value = signal; revision.value++; },
      onResync: () => { lastSignal.value = null; revision.value++; },
    });
  };
  return { storeId, status, revision, lastSignal, start, stop };
});
