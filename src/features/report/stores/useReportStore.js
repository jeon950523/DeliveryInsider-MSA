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
    try {
      isExporting.value = true;
      const response = await downloadReportXlsxApi({
        ...buildAnalysisParams(filters),
        ...(filters.status && { status: filters.status }),
        ...(filters.keyword && { keyword: filters.keyword.trim() }),
        ...(filters.historyType && { historyType: filters.historyType }),
      });
      const blob = response.data;

      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = 'DeliveryInsider_Report.xlsx';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error(error);
      alert(error?.response?.status === 403 ? 'Excel 내보내기는 Standard 기능입니다.' : 'Excel 다운로드에 실패했습니다.');
      throw error;
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
