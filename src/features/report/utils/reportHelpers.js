import { formatDurationSeconds, formatKstDate } from '../../../shared/utils/timeFormatters.js';

export const processingMetrics = [
  { key: 'totalProcessing', label: '평균 전체 처리시간' },
  { key: 'waiting', label: '평균 접수 대기시간' },
  { key: 'cooking', label: '평균 조리시간' },
  { key: 'pickupWaiting', label: '평균 픽업 대기시간' },
  { key: 'delivery', label: '평균 배달시간' },
];
export const platformNames = {
  BAEMIN: '배민', COUPANG_EATS: '쿠팡이츠', YOGIYO: '요기요', DDANGYO: '땡겨요',
};
// Report /orders는 Provider 상태를 반환한다. 매장 운영 상태로 변환하지 않는다.
export const reportStatusNames = {
  CREATED: '주문 생성', PICKED_UP: '픽업 완료', DELIVERED: '배달 완료', CANCELED: '취소',
};
export const formatReportMoney = (value) => {
  if (value == null || value === '' || !Number.isFinite(Number(value))) return '-';
  return `${Number(value).toLocaleString('ko-KR')}원`;
};
export const formatProcessingMetric = (metric) => {
  if (!(metric?.sampleCount > 0) || metric.averageSeconds == null
    || !Number.isFinite(Number(metric.averageSeconds)) || Number(metric.averageSeconds) < 0) return '-';
  return formatDurationSeconds(metric.averageSeconds, { zeroAsLessThanSecond: true });
};
export const financialStatusText = (status) => {
  if (!status || status === 'UNAVAILABLE') return '플랫폼 비용 미확보';
  return status === 'AVAILABLE' ? '플랫폼 비용 확보' : status;
};
export const normalizeFilters = (filters = {}) => ({
  startDate: filters.startDate || '', endDate: filters.endDate || '',
  platform: filters.platform || '', status: filters.status || '',
  keyword: String(filters.keyword || '').trim(),
});
const dateBoundary = (value, endOfDay) => {
  if (!value) return undefined;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new Error('날짜 형식을 확인해 주세요.');
  const date = new Date(`${value}T${endOfDay ? '23:59:59.999' : '00:00:00.000'}+09:00`);
  if (Number.isNaN(date.getTime()) || formatKstDate(date) !== value) throw new Error('유효한 날짜를 선택해 주세요.');
  // 현재 DB/API의 마이크로초 정밀도까지 종료일에 포함한다.
  return endOfDay ? `${date.toISOString().slice(0, 19)}.999999` : date.toISOString().slice(0, -1);
};
export const buildAnalysisParams = (filters) => {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) throw new Error('시작일은 종료일보다 늦을 수 없습니다.');
  const from = dateBoundary(filters.startDate, false);
  const to = dateBoundary(filters.endDate, true);
  return { ...(from && { from }), ...(to && { to }), ...(filters.platform && { platformType: filters.platform }) };
};
export const filterReportOrders = (orders, filters = {}) => {
  const keyword = String(filters.keyword || '').trim().toLowerCase();
  return orders.filter((order) => {
    if (filters.status && order.status !== filters.status) return false;
    return !keyword || [`ORD-${order.orderId}`, order.platformOrderId, order.platformType]
      .some((value) => String(value || '').toLowerCase().includes(keyword));
  });
};
const csvEscape = (value) => {
  let text = value == null ? '' : String(value);
  if (typeof value === 'string' && (/^\s*[=+\-@]/.test(text) || /^[\t\r\n]/.test(text))) text = `'${text}`;
  return `"${text.replace(/"/g, '""')}"`;
};
export const createOrdersCsv = (orders) => {
  const rows = [['내부 주문번호', '플랫폼 주문번호', '플랫폼', '플랫폼 상태', '주문금액', '고객 실결제액', '정산정보 상태', '주문일시(UTC)'],
    ...orders.map((order) => [`ORD-${order.orderId}`, order.platformOrderId, order.platformType,
      order.status, order.grossOrderAmount, order.customerPaidAmount, order.financialDataStatus, order.orderedAt])];
  return '\uFEFF' + rows.map((row) => row.map(csvEscape).join(',')).join('\r\n');
};
