// Provider 원문/HTTP 오류 객체에는 결제 키가 포함될 수 있다. 화면에는 정해진 문구만 사용한다.
const messages = {
  'BILLING-001': '구독 정보를 찾을 수 없습니다.',
  'BILLING-101': '결제 정보를 찾을 수 없습니다.',
  'BILLING-102': '결제 금액이 일치하지 않습니다. 구독 관리에서 다시 확인해 주세요.',
  'BILLING-103': '현재 결제 상태를 확인한 후 다시 시도해 주세요.',
  'BILLING-003': '이미 구독이 존재합니다. 구독 관리에서 상태를 확인해 주세요.',
  'BILLING-104': '현재 구독 상태에서는 해지할 수 없습니다.',
  'BILLING-105': '처리 중인 결제가 있습니다. 결과 확인 후 다시 시도해 주세요.',
  USER_CANCEL: '결제를 취소했습니다. 구독 관리에서 다시 시도할 수 있습니다.',
  PAY_PROCESS_CANCELED: '결제를 취소했습니다. 구독 관리에서 다시 시도할 수 있습니다.',
  PAY_PROCESS_ABORTED: '결제가 완료되지 않았습니다. 결제 수단을 확인하고 다시 시도해 주세요.',
  REJECT_CARD_COMPANY: '카드사에서 결제를 승인하지 않았습니다. 다른 결제 수단으로 다시 시도해 주세요.',
};
export const billingMessage = (code, fallback = '결제가 완료되지 않았습니다. 구독 관리에서 상태를 확인해 주세요.') =>
  Object.hasOwn(messages, code) ? messages[code] : fallback;
export const billingErrorMessage = (error, fallback) =>
  billingMessage(error?.response?.data?.code ?? error?.code, fallback);
