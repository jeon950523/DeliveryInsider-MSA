import axios from 'axios';

const publicHttpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

const moveToServerErrorPage = () => {
  if (window.location.pathname === '/error') {
    return;
  }

  window.location.assign('/error');
};

publicHttpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const shouldMoveToErrorPage = !error.response || status >= 500;

    if (shouldMoveToErrorPage) {
      moveToServerErrorPage();
    }

    return Promise.reject(error);
  }
);

export default publicHttpClient;
