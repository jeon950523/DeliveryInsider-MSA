import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  fetchReportOrders,
  fetchReportProcessingTimes,
  fetchReportSummary,
} from '../api/reportApi.js';
import {
  formatKstDate,
  formatKstDateTime,
} from '../../../shared/utils/timeFormatters.js';

const createEmptySummary = () => ({
  totalOrderCount: 0,
  completedOrderCount: 0,
  canceledOrderCount: 0,
  grossOrderAmount: 0,
  customerPaidAmount: 0,
  providerChargeAmount: 0,
  estimatedMenuCost: 0,
  estimatedPackagingCost: 0,
  financialDataStatuses: [],
});

const createEmptyMetric = () => ({
  sampleCount: 0,
  averageSeconds: null,
});

const createEmptyProcessingTimes = () => ({
  completedOrderCount: 0,
  totalProcessing: createEmptyMetric(),
  waiting: createEmptyMetric(),
  cooking: createEmptyMetric(),
  pickupWaiting: createEmptyMetric(),
  delivery: createEmptyMetric(),
  platforms: [],
});

const PROVIDER_STATUS_TO_UI_STATUS = {
  CREATED: 'WAITING',
  PICKED_UP: 'DELIVERING',
  DELIVERED: 'COMPLETED',
  CANCELED: 'CANCELED',
};

export const useReportStore = defineStore('report', () => {
  const reportOrders = ref([]);
  const allReportOrders = ref([]);
  const reportSummary = ref(createEmptySummary());
  const processingTimes = ref(createEmptyProcessingTimes());

  const isLoading = ref(false);
  const isExporting = ref(false);
  const lastSearchParams = ref({});

  /*
   * Report DB는 UTC LocalDateTime을 저장한다.
   * 사용자가 선택한 한국 날짜 경계를 UTC LocalDateTime으로 변환해서 보낸다.
   */
  const toUtcLocalDateTime = (dateText, endOfDay = false) => {
    if (!dateText) {
      return null;
    }

    const localTime = endOfDay
      ? '23:59:59.999'
      : '00:00:00.000';

    const date = new Date(`${dateText}T${localTime}+09:00`);

    if (Number.isNaN(date.getTime())) {
      return null;
    }

    return date.toISOString().slice(0, 19);
  };

  const buildAnalysisParams = (filters = {}) => {
    const params = {};

    const from = toUtcLocalDateTime(filters.startDate);
    const to = toUtcLocalDateTime(filters.endDate, true);

    if (from) {
      params.from = from;
    }

    if (to) {
      params.to = to;
    }

    const platformType = filters.platformType || filters.platform;

    if (platformType) {
      params.platformType = platformType;
    }

    return params;
  };

  const buildOrderParams = (filters = {}) => ({
    ...buildAnalysisParams(filters),
    page: 0,
    size: 100,
    sortBy: 'orderedAt',
    direction: 'desc',
  });

  const formatDate = (dateTime) => formatKstDate(dateTime, '');
  const formatDateTime = (dateTime) => formatKstDateTime(dateTime, '');

  const normalizeReportOrder = (order = {}) => {
    const orderStatus =
      PROVIDER_STATUS_TO_UI_STATUS[order.status]
      || order.status
      || '';

    return {
      ...order,
      id: order.orderId,
      orderNo: order.orderId ? `ORD-${order.orderId}` : '',
      platformOrderNo: order.platformOrderId || '',
      platformOrderNumber: order.platformOrderId || '',
      orderStatus,
      totalAmount: order.grossOrderAmount == null
        ? null
        : Number(order.grossOrderAmount),
      customerPaidAmount: order.customerPaidAmount,
      financialDataStatus: order.financialDataStatus || 'UNAVAILABLE',
      orderDate: formatDate(order.orderedAt),
      orderedAtText: formatDateTime(order.orderedAt),

      // 현재 Report 주문 목록 API가 제공하지 않는 값은 추정하지 않는다.
      menuSummary: '',
      menuCostAmount: null,
      packagingAmount: null,
      commissionAmount: null,
      deliveryFeeAmount: null,
      couponAmount: null,
      platformSupportAmount: null,
      netProfit: null,
      requestText: '',
      requestRiskType: '',
      requestRiskLevel: '',
      riskBadges: [],
      lossRisk: false,
      cancelType: '',
      cancelReason: '',
      refundType: '',
      refundReason: '',
      completedAt: '',
      canceledAt: '',
      refundedAt: '',
    };
  };

  const applyClientReportFilter = (orders, filters = {}) => {
    const keyword = String(filters.keyword || '').trim().toLowerCase();

    return orders.filter((order) => {
      if (filters.status && order.orderStatus !== filters.status) {
        return false;
      }

      if (keyword) {
        const target = [
          order.orderNo,
          order.platformOrderNo,
          order.platformType,
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();

        if (!target.includes(keyword)) {
          return false;
        }
      }

      if (filters.risk === 'CANCEL') {
        return order.orderStatus === 'CANCELED';
      }

      if (filters.risk === 'REFUND') {
        return order.orderStatus === 'REFUNDED';
      }

      // REQUEST / LOSS는 현재 Report Read API가 근거 데이터를 제공하지 않는다.
      if (filters.risk === 'REQUEST' || filters.risk === 'LOSS') {
        return false;
      }

      return true;
    });
  };

  const findOrders = async (filters = {}) => {
    const params = buildOrderParams(filters);
    const result = await fetchReportOrders(params);
    const payload = result.data || {};
    const content = Array.isArray(payload)
      ? payload
      : Array.isArray(payload.content)
        ? payload.content
        : [];

    const normalizedOrders = content.map(normalizeReportOrder);

    allReportOrders.value = normalizedOrders;

    reportOrders.value = applyClientReportFilter(
      allReportOrders.value,
      filters
    );

    return reportOrders.value;
  };

  const findSummary = async (filters = {}) => {
    const params = buildAnalysisParams(filters);
    const result = await fetchReportSummary(params);

    reportSummary.value = {
      ...createEmptySummary(),
      ...(result.data || {}),
      financialDataStatuses:
        result.data?.financialDataStatuses || [],
    };

    return reportSummary.value;
  };

  const findProcessingTimes = async (filters = {}) => {
    const params = buildAnalysisParams(filters);
    const result = await fetchReportProcessingTimes(params);

    processingTimes.value = {
      ...createEmptyProcessingTimes(),
      ...(result.data || {}),
      platforms: result.data?.platforms || [],
    };

    return processingTimes.value;
  };

  const findReports = async (filters = {}) => {
    try {
      isLoading.value = true;

      const params = buildAnalysisParams(filters);
      lastSearchParams.value = { ...params };

      const [orders] = await Promise.all([
        findOrders(filters),
        findSummary(filters),
        findProcessingTimes(filters),
      ]);

      return orders;
    } catch (error) {
      console.error(error);
      alert('운영 리포트 조회에 실패했습니다.');
      throw error;
    } finally {
      isLoading.value = false;
    }
  };

  const csvEscape = (value) => {
    const text = value === null || value === undefined
      ? ''
      : String(value);

    return `"${text.replace(/"/g, '""')}"`;
  };

  /*
   * Report Backend에는 현재 CSV export endpoint가 없다.
   * 존재하지 않는 API를 호출하지 않고, 화면에 조회된 실제 데이터만 CSV로 저장한다.
   */
  const downloadOrdersCsv = async (filters = {}) => {
    try {
      isExporting.value = true;

      const rows = applyClientReportFilter(
        allReportOrders.value,
        filters
      );

      const headers = [
        '내부 주문번호',
        '플랫폼 주문번호',
        '플랫폼',
        '상태',
        '주문금액',
        '고객 실결제액',
        '정산정보 상태',
        '주문일시',
      ];

      const bodyRows = rows.map((order) => [
        order.orderNo,
        order.platformOrderNo,
        order.platformType,
        order.orderStatus,
        order.totalAmount ?? '',
        order.customerPaidAmount ?? '',
        order.financialDataStatus,
        order.orderedAtText,
      ]);

      const csv = '\uFEFF' + [headers, ...bodyRows]
        .map((row) => row.map(csvEscape).join(','))
        .join('\n');

      const blob = new Blob([csv], {
        type: 'text/csv;charset=utf-8;',
      });

      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');

      link.href = downloadUrl;
      link.download = `deliveryinsider-orders-${new Date()
        .toISOString()
        .slice(0, 10)}.csv`;

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error(error);
      alert('CSV 다운로드에 실패했습니다.');
      throw error;
    } finally {
      isExporting.value = false;
    }
  };

  const clearReports = () => {
    reportOrders.value = [];
    allReportOrders.value = [];
    reportSummary.value = createEmptySummary();
    processingTimes.value = createEmptyProcessingTimes();
    isLoading.value = false;
    isExporting.value = false;
    lastSearchParams.value = {};
  };

  return {
    reportOrders,
    reportSummary,
    processingTimes,
    isLoading,
    isExporting,
    lastSearchParams,

    findOrders,
    findSummary,
    findProcessingTimes,
    findReports,
    downloadOrdersCsv,
    clearReports,
  };
});
