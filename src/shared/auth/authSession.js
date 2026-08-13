let accessToken = '';
const tokenListeners = new Set();

export const getAccessToken = () => accessToken;

export const setAccessToken = (token = '') => {
  accessToken = token || '';
  tokenListeners.forEach((listener) => listener(accessToken));
};

export const clearAccessToken = () => {
  setAccessToken('');
};

export const subscribeAccessToken = (listener) => {
  tokenListeners.add(listener);
  return () => tokenListeners.delete(listener);
};
