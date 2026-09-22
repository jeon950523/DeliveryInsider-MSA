import axios from 'axios';
import { jwtDecode } from 'jwt-decode';
import dayjs from 'dayjs';
import publicHttpClient from './publicHttpClient.js';

import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '../auth/authSession.js';

const httpClient = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL
    || 'http://localhost:8090',

  withCredentials: true,

  headers: {
    'Content-Type': 'application/json',
  },
});

const AUTH_REQUIRED_ERROR_CODE =
  'AUTH_REQUIRED';

let reissuePromise = null;

const clearLoginState = () => {
  clearAccessToken();
  localStorage.removeItem(
    'hasLoginHint'
  );
};

const moveToLoginPage = () => {
  const currentPath =
    window.location.pathname;

  if (
    currentPath === '/login'
    || currentPath === '/register'
    || currentPath === '/'
  ) {
    return;
  }

  window.location.replace(
    '/login'
  );
};

const moveToServerErrorPage = () => {
  if (
    window.location.pathname
    === '/error'
  ) {
    return;
  }

  window.location.assign(
    '/error'
  );
};

const createAuthRequiredError = () => {
  const error =
    new Error(
      '로그인이 필요합니다.'
    );

  error.code =
    AUTH_REQUIRED_ERROR_CODE;

  return error;
};

export const reissueAccessToken =
  async () => {

    if (reissuePromise) {
      return reissuePromise;
    }

    reissuePromise =
      (async () => {

        try {
          const response =
            await publicHttpClient.post(
              '/api/auth/reissue-token'
            );

          const token =
            response.data
              ?.data
              ?.accessToken
            || '';

          if (!token) {
            clearLoginState();
            return false;
          }

          setAccessToken(
            token
          );

          return true;

        } catch (error) {
          clearLoginState();
          return false;

        } finally {
          reissuePromise = null;
        }
      })();

    return reissuePromise;
  };

const isNearExpiration = (
  token
) => {

  try {
    const claims =
      jwtDecode(
        token
      );

    if (!claims?.exp) {
      return true;
    }

    const now =
      dayjs().unix();

    const refreshThreshold =
      dayjs
        .unix(
          claims.exp
        )
        .subtract(
          5,
          'minute'
        )
        .unix();

    return now
      >= refreshThreshold;

  } catch (error) {
    return true;
  }
};

httpClient.interceptors.request.use(
  async (config) => {

    let accessToken =
      getAccessToken();

    if (
      !accessToken
      || isNearExpiration(
        accessToken
      )
    ) {

      const reissueSuccess =
        await reissueAccessToken();

      if (!reissueSuccess) {

        clearLoginState();
        moveToLoginPage();

        /*
         * 중요:
         *
         * Refresh 실패 후에도
         * 원래 보호 API를 보내던 기존 동작을 막는다.
         */
        return Promise.reject(
          createAuthRequiredError()
        );
      }

      accessToken =
        getAccessToken();
    }

    if (accessToken) {
      config.headers.Authorization =
        `Bearer ${accessToken}`;
    }

    return config;
  }
);

httpClient.interceptors.response.use(
  (response) =>
    response,

  (error) => {

    /*
     * Request Interceptor에서
     * 의도적으로 중단한 인증 실패 요청.
     *
     * 이걸 Network Error로 보고
     * /error 페이지로 보내면 안 된다.
     */
    if (
      error?.code
      === AUTH_REQUIRED_ERROR_CODE
    ) {
      return Promise.reject(
        error
      );
    }

    const status =
      error.response?.status;

    /*
     * Access Token이 만료/폐기되는 등
     * 보호 API가 실제 401을 반환한 경우.
     */
    if (status === 401) {

      clearLoginState();
      moveToLoginPage();

      return Promise.reject(
        error
      );
    }

    /*
     * 실제 서버 장애 또는
     * Backend 연결 자체가 불가능한 경우만
     * Server Error 화면으로 이동한다.
     */
    const shouldMoveToErrorPage =
      error.config?.skipServerErrorRedirect !== true
      && (
        !error.response
        || status >= 500
      );

    if (
      shouldMoveToErrorPage
    ) {
      moveToServerErrorPage();
    }

    return Promise.reject(
      error
    );
  }
);

export default httpClient;
