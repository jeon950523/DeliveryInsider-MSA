import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  fetchReportOrders,
  fetchReportHistory,
  fetchEstimatedMenuProfit,
  fetchReportDailyTrend,
  fetchReportProcessingTimes,
  fetchReportPlatformMetrics,
  fetchReportSummary,
  downloadReportXlsx as downloadReportXlsxApi,
} from '../api/reportApi.js';
import {
  formatKstDate,
  formatKstDateTime,
} from '../../../shared/utils/timeFormatters.js';
import {
  buildAnalysisParams as buildReportAnalysisParams,
} from '../utils/reportHelpers.js';

const createEmptySummary = () => ({
  totalOrderCount: 0,
  completedOrderCount: 0,
  canceledOrderCount: 0,
  grossOrderAmount: 0,
  customerPaidAmount: 0,
  providerChargeAmount: 0,
  estimatedMenuCost: 0,
  estimatedPackagingCost: 0,
  estimatedNetProfit: 0,
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

const XLSX_CONTENT_TYPE =
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const readContentDispositionFilename = (value) => {
  const header = String(value || '');
  const encoded = header.match(/filename\*=UTF-8''([^;]+)/i);

  if (encoded?.[1]) {
    try {
      return decodeURIComponent(encoded[1]);
    } catch {
      return 'DeliveryInsider_Report.xlsx';
    }
  }

  const plain = header.match(/filename="?([^";]+)"?/i);
  return plain?.[1] || 'DeliveryInsider_Report.xlsx';
};

const getExportErrorMessage = async (error) => {
  if (error?.response?.status === 403) {
    return '현재 요금제에서는 Excel 내보내기를 사용할 수 없습니다.';
  }

  const body = error?.response?.data;
  if (body instanceof Blob) {
    try {
      const payload = JSON.parse(await body.text());
      return payload?.message || payload?.detail || 'Excel 파일을 생성하지 못했습니다.';
    } catch {
      // 오류 본문 형식을 신뢰할 수 없으면 사용자에게 내부 내용을 노출하지 않는다.
    }
  }

  return 'Excel 파일을 생성하지 못했습니다.';
};

export const useReportStore = defineStore('report', () => {
  const reportOrders = ref([]);
  const allReportOrders = ref([]);
  const reportHistory = ref([]);
  const reportSummary = ref(createEmptySummary());
  const processingTimes = ref(createEmptyProcessingTimes());
  const estimatedMenuProfits = ref([]);
  const dailyTrend = ref([]);
  const platformMetrics = ref([]);

  const isLoading = ref(false);
  const isExporting = ref(false);
  const exportError = ref('');
  const hasLoaded = ref(false);
  const loadError = ref('');
  const lastSearchParams = ref({});

  const buildAnalysisParams = (filters = {}) => buildReportAnalysisParams({
    ...filters,
    platform: filters.platformType || filters.platform || '',
  });

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

  const normalizeReportHistory = (history = {}) => ({
    ...history,
    id: `${history.historyType || 'HISTORY'}-${history.orderId || ''}`,
    orderNo: history.orderId ? `ORD-${history.orderId}` : '',
    platformOrderNo: history.platformOrderId || '',
    occurredDate: formatDate(history.occurredAt),
    occurredAtText: formatDateTime(history.occurredAt),
    amount: history.amount == null ? null : Number(history.amount),
  });

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

  const applyHistoryClientFilter = (history, filters = {}) => {
    const keyword = String(filters.keyword || '').trim().toLowerCase();

    if (!keyword) {
      return history;
    }

    return history.filter((entry) => {
      const target = [
        entry.orderNo,
        entry.platformOrderNo,
        entry.platformType,
        entry.reasonCode,
        entry.reasonText,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return target.includes(keyword);
    });
  };

  const findHistory = async (filters = {}) => {
    const result = await fetchReportHistory(buildAnalysisParams(filters));
    const normalizedHistory = Array.isArray(result.data)
      ? result.data.map(normalizeReportHistory)
      : [];
    reportHistory.value = applyHistoryClientFilter(normalizedHistory, filters);
    return reportHistory.value;
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

  const findEstimatedMenuProfits = async (filters = {}) => {
    const result = await fetchEstimatedMenuProfit(buildAnalysisParams(filters));
    estimatedMenuProfits.value = Array.isArray(result.data) ? result.data : [];
    return estimatedMenuProfits.value;
  };

  const findDailyTrend = async (filters = {}) => {
    const result = await fetchReportDailyTrend(buildAnalysisParams(filters));
    dailyTrend.value = Array.isArray(result.data) ? result.data : [];
    return dailyTrend.value;
  };

  const findPlatformMetrics = async (filters = {}) => {
    const result = await fetchReportPlatformMetrics(buildAnalysisParams(filters));
    platformMetrics.value = Array.isArray(result.data) ? result.data : [];
    return platformMetrics.value;
  };

  const findReports = async (filters = {}) => {
    try {
      isLoading.value = true;
      loadError.value = '';

      const params = buildAnalysisParams(filters);
      lastSearchParams.value = { ...params };

      const [orders] = await Promise.all([
        findOrders(filters),
        findHistory(filters),
        findSummary(filters),
        findProcessingTimes(filters),
        findEstimatedMenuProfits(filters),
        findDailyTrend(filters),
        findPlatformMetrics(filters),
      ]);

      hasLoaded.value = true;
      return orders;
    } catch (error) {
      console.error(error);
      hasLoaded.value = false;
      loadError.value = '잠시 후 다시 조회해 주세요.';
      throw error;
    } finally {
      isLoading.value = false;
    }
  };

  const downloadReportXlsx = async (filters = {}) => {
    if (isExporting.value) {
      return false;
    }

    try {
      isExporting.value = true;
      exportError.value = '';
      const response = await downloadReportXlsxApi({
        ...buildAnalysisParams(filters),
        ...(filters.status && { status: filters.status }),
        ...(filters.keyword && { keyword: filters.keyword.trim() }),
        ...(filters.historyType && { historyType: filters.historyType }),
      }, {
        skipServerErrorRedirect: true,
      });
      const blob = response.data;

      const contentType = String(response.headers?.['content-type'] || '')
        .toLowerCase();

      if (!(blob instanceof Blob) || !contentType.includes(XLSX_CONTENT_TYPE)) {
        throw new Error('XLSX 응답이 아닙니다.');
      }

      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = readContentDispositionFilename(
        response.headers?.['content-disposition'],
      );
      document.body.appendChild(link);
      link.click();
      link.remove();

      // 브라우저가 anchor click을 다운로드 작업으로 넘긴 뒤에만 해제한다.
      window.setTimeout(() => window.URL.revokeObjectURL(downloadUrl), 1_000);
      return true;
    } catch (error) {
      console.error(error);
      exportError.value = await getExportErrorMessage(error);
      return false;
    } finally {
      isExporting.value = false;
    }
  };

  const clearReports = () => {
    reportOrders.value = [];
    allReportOrders.value = [];
    reportHistory.value = [];
    reportSummary.value = createEmptySummary();
    processingTimes.value = createEmptyProcessingTimes();
    estimatedMenuProfits.value = [];
    dailyTrend.value = [];
    platformMetrics.value = [];
    isLoading.value = false;
    isExporting.value = false;
    exportError.value = '';
    hasLoaded.value = false;
    loadError.value = '';
    lastSearchParams.value = {};
  };

  return {
    reportOrders,
    reportHistory,
    reportSummary,
    processingTimes,
    estimatedMenuProfits,
    dailyTrend,
    platformMetrics,
    isLoading,
    isExporting,
    exportError,
    hasLoaded,
    loadError,
    lastSearchParams,

    findOrders,
    findHistory,
    findSummary,
    findProcessingTimes,
    findEstimatedMenuProfits,
    findDailyTrend,
    findPlatformMetrics,
    findReports,
    downloadReportXlsx,
    clearReports,
  };
});
