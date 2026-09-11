const eventTypes = new Set(['ORDER_CREATED', 'ORDER_OPERATION_STATUS_CHANGED', 'ORDER_STATUS_CHANGED', 'ORDER_CANCELED']);

export function validOrderSignal(signal, storeId) {
  return Boolean(signal && signal.storeId === storeId && Number.isSafeInteger(signal.orderId) && signal.orderId > 0
    && Number.isSafeInteger(signal.eventVersion) && signal.eventVersion > 0 && typeof signal.eventId === 'string'
    && signal.eventId.length > 0 && eventTypes.has(signal.eventType));
}

export function webSocketUrl(gateway, ticket) {
  const url = new URL(gateway);
  if (!['http:', 'https:'].includes(url.protocol)) throw new Error('지원하지 않는 Gateway 주소입니다.');
  url.pathname = '/ws'; url.search = ''; url.hash = '';
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.searchParams.set('ticket', ticket);
  return url.href;
}

// Only reconnects the transport. This manager never polls Order/Report data.
export function createOrderConnection({ storeId, gateway, issueTicket, makeClient, onSignal, onStatus, onResync,
  setTimer = setTimeout, clearTimer = clearTimeout }) {
  let stopped = false;
  let generation = 0;
  let client;
  let retryTimer;
  let failures = 0;
  const versions = new Map();
  const dispose = (target) => { if (target) Promise.resolve(target.deactivate({ force: true })).catch(() => {}); };
  const retry = () => {
    if (stopped || retryTimer != null) return;
    onStatus('reconnecting');
    const delay = Math.min(30000, 1000 * 2 ** Math.min(failures++, 5));
    retryTimer = setTimer(() => { retryTimer = undefined; void connect(); }, delay);
  };
  async function connect() {
    const attempt = ++generation;
    const current = () => !stopped && attempt === generation;
    onStatus('connecting');
    try {
      // One-use ticket is never persisted, logged, or reused on a new connection.
      const ticket = await issueTicket();
      if (!current()) return;
      if (ticket?.storeId !== storeId || typeof ticket.ticket !== 'string' || !ticket.ticket) {
        stopped = true; onStatus('scope-error'); return;
      }
      const connection = makeClient({
        brokerURL: webSocketUrl(gateway, ticket.ticket), reconnectDelay: 0, connectionTimeout: 8000,
        heartbeatIncoming: 10000, heartbeatOutgoing: 10000, debug: () => {}, logRawCommunication: false,
        onConnect: () => {
          if (!current()) { dispose(connection); return; }
          connection.subscribe(`/topic/stores/${storeId}/orders`, (message) => {
            if (!current()) return;
            let signal;
            try { signal = JSON.parse(message.body); } catch { return; }
            if (!validOrderSignal(signal, storeId) || (versions.get(signal.orderId) || 0) >= signal.eventVersion) return;
            versions.delete(signal.orderId); versions.set(signal.orderId, signal.eventVersion);
            if (versions.size > 1000) versions.delete(versions.keys().next().value);
            onSignal(signal);
          }, { ack: 'auto' });
          failures = 0; onStatus('connected');
          onResync(); // One snapshot refresh closes changes missed while disconnected.
        },
        onWebSocketClose: () => { if (current()) retry(); },
        onWebSocketError: () => { if (current()) onStatus('reconnecting'); },
        onStompError: () => { if (current()) { dispose(connection); retry(); } },
      });
      dispose(client); client = connection;
      connection.activate();
    } catch {
      if (current()) retry();
    }
  }
  void connect();
  return { stop() {
    stopped = true; generation++;
    if (retryTimer != null) clearTimer(retryTimer);
    retryTimer = undefined; dispose(client); client = undefined; versions.clear();
  } };
}

// Coalesce a burst and allow at most one trailing refresh while a request is running.
export function createCoalescedRefresh(refresh, { delay = 100, onError = () => {}, setTimer = setTimeout, clearTimer = clearTimeout } = {}) {
  let timer; let running = false; let dirty = false; let stopped = false;
  const request = () => {
    if (stopped) return;
    dirty = true;
    if (timer != null || running) return;
    timer = setTimer(async () => {
      timer = undefined;
      if (stopped) return;
      running = true; dirty = false;
      try { await refresh(); } catch (error) { if (!stopped) onError(error); }
      finally { running = false; if (dirty && !stopped) request(); }
    }, delay);
  };
  return { request, stop() { stopped = true; dirty = false; if (timer != null) clearTimer(timer); timer = undefined; } };
}
