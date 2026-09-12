const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL
  || 'http://localhost:8090';

const KAKAO_ERROR_MESSAGES = {
  'AUTH-016': '카카오 로그인 설정을 확인해 주세요.',
  'AUTH-017': '카카오 로그인 요청이 만료되었거나 올바르지 않습니다. 다시 시도해 주세요.',
  'AUTH-018': '카카오 로그인이 취소되었습니다.',
  'AUTH-019': '카카오 계정 이메일 제공 동의가 필요합니다.',
  'AUTH-020': '이 계정에는 다른 카카오 계정이 이미 연결되어 있습니다.',
  'AUTH-021': '카카오 로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.',
};

export const kakaoAuthorizationUrl = () =>
  `${API_BASE_URL}/api/auth/oauth2/authorization/kakao`;

export const oauthErrorMessage = (code) =>
  KAKAO_ERROR_MESSAGES[code]
  || '카카오 로그인에 실패했습니다. 다시 시도해 주세요.';
