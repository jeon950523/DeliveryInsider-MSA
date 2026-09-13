import {
  createRouter,
  createWebHistory,
} from 'vue-router';

import { useAuthStore } from '../../features/auth/stores/useAuthStore.js';
import { useStoreStore } from '../../features/store/stores/useStoreStore.js';

import {
  isStoreNotFoundError,
} from '../../features/onboarding/utils/storeOnboarding.js';
import {
  resolveOnboardingTarget,
} from '../../features/onboarding/utils/onboardingPolicy.js';

import BillingView from '../../features/billing/views/BillingView.vue';
import BillingPaymentSuccessView from '../../features/billing/views/BillingPaymentSuccessView.vue';
import BillingPaymentFailView from '../../features/billing/views/BillingPaymentFailView.vue';
import LandingView from '../../features/landing/views/LandingView.vue';
import Login from '../../features/auth/views/LoginView.vue';
import Register from '../../features/auth/views/RegisterView.vue';
import PhoneOnboardingView from '../../features/auth/views/PhoneOnboardingView.vue';
import OAuthCallbackView from '../../features/auth/views/OAuthCallbackView.vue';
import StoreOnboardingView from '../../features/onboarding/views/StoreOnboardingView.vue';

import DashboardView from '../../features/dashboard/views/DashboardView.vue';
import MenusView from '../../features/menu/views/MenusView.vue';
import StoreView from '../../features/store/views/StoreView.vue';

import ProfileView from '../../features/profile/views/ProfileView.vue';
import OrdersView from '../../features/order/views/OrdersView.vue';
import NotFoundView from '../error/NotFoundView.vue';
import ServerErrorView from '../error/ServerErrorView.vue';
import AdminLayout from '../../features/admin/components/AdminLayout.vue';
import AdminDashboardView from '../../features/admin/views/AdminDashboardView.vue';
import AdminUsersView from '../../features/admin/views/AdminUsersView.vue';
import AdminStoresView from '../../features/admin/views/AdminStoresView.vue';
import AdminPlatformsView from '../../features/admin/views/AdminPlatformsView.vue';
import AdminSubscriptionsView from '../../features/admin/views/AdminSubscriptionsView.vue';

const AllReportView = () => import('../../features/report/views/AllReportView.vue');

/*
 * 라우트마다 사용할 meta 정보를 생성한다.
 *
 * isAuthenticated
 * → 로그인이 필요한 화면인지 표시
 *
 * isGuestOnly
 * → 로그인하지 않은 사용자만 접근 가능한 화면인지 표시
 */
const setMeta = (
  isAuthenticated = false,
  isGuestOnly = false
) => {
  return {
    isAuthenticated,
    isGuestOnly,
  };
};

const routes = [
  {
    path: '/admin',
    component: AdminLayout,
    meta: {
      isAuthenticated: true,
      requiresAdmin: true,
      allowWithoutPhoneVerification: true,
      allowWithoutStore: true,
      hideLayout: true,
    },
    children: [
      { path: '', name: 'admin-dashboard', component: AdminDashboardView, meta: { title: '관리자 운영 현황' } },
      { path: 'users', name: 'admin-users', component: AdminUsersView, meta: { title: '전체 회원' } },
      { path: 'stores', name: 'admin-stores', component: AdminStoresView, meta: { title: '전체 매장' } },
      { path: 'platforms', name: 'admin-platforms', component: AdminPlatformsView, meta: { title: '플랫폼 연결 및 처리 오류' } },
      { path: 'subscriptions', name: 'admin-subscriptions', component: AdminSubscriptionsView, meta: { title: '전체 구독' } },
    ],
  },
  /*
   * 로그인 전 메인 랜딩 페이지
   */
  {
    path: '/',
    name: 'landing',
    component: LandingView,
    meta: { isGuestOnly: true, hideLayout: true }
  },

  /*
   * 로그인 화면
   */
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { isGuestOnly: true, hideLayout: true }
  },

  /*
   * 회원가입 화면
   */
  {
    path: '/register',
    name: 'register',
    component: Register,
    meta: { isGuestOnly: true, hideLayout: true }
  },

  {
    path: '/login/oauth2/callback',
    name: 'oauth-callback',
    component: OAuthCallbackView,
    meta: {
      hideLayout: true,
      title: '카카오 로그인',
    },
  },


  {
    path: '/onboarding/phone',
    name: 'phone-onboarding',
    component: PhoneOnboardingView,
    meta: {
      isAuthenticated: true,
      allowWithoutPhoneVerification: true,
      allowWithoutStore: true,
      hideLayout: true,
      title: '휴대폰 인증',
    },
  },

  /*
   * 신규 사용자 Store Onboarding
   *
   * 로그인은 필요하지만 Store가 없어도 접근할 수 있어야 한다.
   */
  {
    path: '/onboarding/store',
    name: 'store-onboarding',
    component: StoreOnboardingView,
    meta: {
      isAuthenticated: true,
      allowWithoutStore: true,
      hideLayout: true,
      title: '매장 등록',
    },
  },

  /*
   * 오늘 운영 대시보드
   */
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardView,
    meta: { isAuthenticated: true, title: '실시간 운영 대시보드' },
  },

  /*
   * 통합 주문 관리
   */
  {
    path: '/orders',
    name: 'orders',
    component: OrdersView,
    meta: { isAuthenticated: true, title: '통합 주문 관리' },
  },

  /*
   * 메뉴 수익성 설정
   */
  {
    path: '/menus',
    name: 'menus',
    component: MenusView,
    meta: { isAuthenticated: true, title: '메뉴 관리' },
  },

  /*
   * 매장 관리
   *
   * 기존 Store 관리 화면은 A 작업 범위이므로
   * 신규 사용자 Onboarding과 분리한다.
   */
  {
    path: '/store',
    name: 'store',
    component: StoreView,
    meta: { isAuthenticated: true, title: '매장 관리', allowWithoutStore: true },
  },

  /*
   * 운영 리포트
   */
  {
    path: '/reports',
    name: 'reports',
    component: AllReportView,
    meta: { isAuthenticated: true, title: '운영 리포트' },
  },

  {
    path: '/billing',
    name: 'billing',
    component: BillingView,
    meta: {
      isAuthenticated: true,
      title: '구독 관리',
    },
  },

  {
    path: '/billing/payment/success',
    name: 'billing-payment-success',
    component: BillingPaymentSuccessView,
    meta: {
      isAuthenticated: true,
      title: '결제 결과 확인',
      hideLayout: true,
    },
  },

  {
    path: '/billing/payment/fail',
    name: 'billing-payment-fail',
    component: BillingPaymentFailView,
    meta: {
      isAuthenticated: true,
      title: '결제 실패',
      hideLayout: true,
    },
  },


  /*
   * 내 정보 화면
   */
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: { isAuthenticated: true, title: '내 정보', allowWithoutStore: true },
  },

  /*
   * 서버 오류 화면
   */
  {
    path: '/error',
    name: 'server-error',
    component: ServerErrorView,
    meta: { hideLayout: true },
  },

  /*
   * 404 화면
   */
  {
    path: '/not-found',
    name: 'not-found',
    component: NotFoundView,
    meta: { hideLayout: true },
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'catch-all-not-found',
    redirect: { name: 'not-found' },
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

/*
 * 화면 이동 전에 실행되는 네비게이션 가드
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore();

  /*
   * 보호 화면인데 Access Token이 없다면
   * Refresh Token으로 로그인 상태를 먼저 복구한다.
   */
  if (
    to.meta.isAuthenticated &&
    !authStore.accessToken
  ) {
    const reissueSuccess = await authStore.reissue();

    if (!reissueSuccess) {
      return {
        name: 'login',
      };
    }
  }

  /*
   * 복구 후에도 로그인 상태가 아니면 로그인으로 이동한다.
   */
  if (
    to.meta.isAuthenticated &&
    !authStore.isLoggedIn
  ) {
    return {
      name: 'login',
    };
  }

  /*
   * 이미 로그인한 사용자가 Guest 화면에 접근하면
   * 우선 Dashboard 진입을 시도한다.
   * Store가 없으면 아래 Store Guard에서 Onboarding으로 전환된다.
   */
  if (
    to.meta.isGuestOnly &&
    authStore.isLoggedIn
  ) {
    return {
      name: authStore.isAdmin ? 'admin-dashboard' : 'dashboard',
    };
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'dashboard' };
  }

  if (
    to.meta.isAuthenticated &&
    authStore.isAdmin &&
    !to.meta.requiresAdmin
  ) {
    return { name: 'admin-dashboard' };
  }


  /*
   * 로그인 사용자는 Store Onboarding보다 먼저 휴대폰 인증을 완료한다.
   * 휴대폰 인증 API는 SCG JWT가 검증한 X-User-Id를 사용한다.
   */
  if (
    to.meta.isAuthenticated &&
    authStore.isLoggedIn &&
    !to.meta.allowWithoutPhoneVerification
  ) {
    try {
      const profile = authStore.userProfile
        || await authStore.fetchMyProfile();

      if (!profile?.phoneVerifiedAt) {
        return {
          name: 'phone-onboarding',
        };
      }
    } catch (error) {
      if (
        error?.code === 'AUTH_REQUIRED' ||
        error?.response?.status === 401
      ) {
        return {
          name: 'login',
        };
      }

      console.error(
        '휴대폰 인증 상태 확인 실패:',
        error
      );

      return {
        name: 'server-error',
      };
    }
  }

  /*
   * Store가 필요한 보호 화면.
   *
   * 신규 사용자의 GET /api/stores/me → 404 STORE-001 은
   * 장애가 아니라 정상 Onboarding 상태다.
   */
  const shouldCheckStore =
    to.meta.isAuthenticated
    && authStore.isLoggedIn
    && (
      !to.meta.allowWithoutStore
      || to.name === 'store-onboarding'
    );

  if (shouldCheckStore) {
    const storeStore = useStoreStore();

    try {
      const myStore = await storeStore.checkMyStore();
      const onboardingTarget = resolveOnboardingTarget({
        hasStore: Boolean(myStore),
      });

      if (onboardingTarget === 'store-onboarding') {
        if (to.name === 'store-onboarding') {
          return true;
        }

        return {
          name: 'store-onboarding',
        };
      }

      if (to.name === 'store-onboarding') {
        return {
          name: 'dashboard',
        };
      }
    } catch (error) {
      if (isStoreNotFoundError(error)) {
        return {
          name: 'store-onboarding',
        };
      }

      if (
        error?.code === 'AUTH_REQUIRED' ||
        error?.response?.status === 401
      ) {
        return {
          name: 'login',
        };
      }

      console.error(
        '매장 상태 확인 실패:',
        error
      );

      return {
        name: 'server-error',
      };
    }
  }

  return true;
});

export default router;
