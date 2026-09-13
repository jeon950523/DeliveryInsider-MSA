<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue';
import { useRouter } from 'vue-router';

const STORAGE_KEY = 'deliveryinsider.firstRunTutorial.v2';
const PANEL_WIDTH = 380;
const VIEWPORT_GAP = 16;

const router = useRouter();
const isVisible = ref(false);
const currentIndex = ref(0);
const targetRect = ref(null);
let activeTarget = null;

const steps = [
  {
    route: '/dashboard',
    selector: '[data-tour="dashboard-summary"]',
    title: '오늘 운영 상태',
    body: '오늘 운영 상태와 진행 주문, 완료 실적을 한눈에 확인합니다.',
  },
  {
    route: '/dashboard',
    selector: '[data-tour="active-orders"]',
    title: '먼저 처리할 주문',
    body: '접수대기·조리·픽업 주문 수를 보고 먼저 들어온 주문부터 처리합니다.',
  },
  {
    route: '/orders',
    selector: '[data-tour="order-management"]',
    title: '통합 주문 관리',
    body: '외부 플랫폼에서 처리되는 주문 상태를 한 화면에서 확인하고 예외 상황을 관리합니다.',
  },
  {
    route: '/store',
    selector: '[data-tour="platform-settings"]',
    title: '매장과 플랫폼 연결',
    body: '외부 배달 플랫폼 Store를 현재 매장에 연결하면 주문이 자동으로 들어옵니다.',
  },
  {
    route: '/menus',
    selector: '[data-tour="menu-navigation"]',
    title: '메뉴 등록과 연결',
    body: '외부 메뉴를 내부 메뉴와 연결해야 주문 처리와 메뉴별 수익 분석이 가능합니다.',
  },
  {
    route: '/reports',
    selector: '[data-tour="report-overview"]',
    title: '운영 리포트',
    body: '기간별 매출과 추정 순수익의 변화를 확인하고 플랫폼·메뉴별 성과를 비교할 수 있습니다.',
  },
  {
    route: '/reports',
    selector: '[data-tour="ai-insights"]',
    title: 'AI 운영 인사이트',
    body: '구독 권한이 있으면 AI가 집계 데이터를 근거로 우선 행동을 제안합니다. 잠긴 상태에서는 이용 가능한 기능으로 표시하지 않습니다.',
  },
];

const currentStep = computed(() => (
  steps[currentIndex.value] || steps[steps.length - 1]
));
const isFirstStep = computed(() => currentIndex.value === 0);
const isLastStep = computed(() => currentIndex.value === steps.length - 1);

const spotlightStyle = computed(() => {
  if (!targetRect.value) return {};

  return {
    top: `${targetRect.value.top - 6}px`,
    left: `${targetRect.value.left - 6}px`,
    width: `${targetRect.value.width + 12}px`,
    height: `${targetRect.value.height + 12}px`,
  };
});

const panelStyle = computed(() => {
  const rect = targetRect.value;

  if (!rect) {
    return {
      left: `${VIEWPORT_GAP}px`,
      bottom: `${VIEWPORT_GAP}px`,
    };
  }

  const width = Math.min(PANEL_WIDTH, window.innerWidth - VIEWPORT_GAP * 2);
  const left = Math.min(
    Math.max(rect.left, VIEWPORT_GAP),
    window.innerWidth - width - VIEWPORT_GAP,
  );
  const estimatedHeight = 260;
  const belowTop = rect.bottom + 18;
  const top = belowTop + estimatedHeight <= window.innerHeight
    ? belowTop
    : Math.max(VIEWPORT_GAP, rect.top - estimatedHeight - 18);

  return {
    top: `${top}px`,
    left: `${left}px`,
    width: `${width}px`,
  };
});

const clearTarget = () => {
  activeTarget?.classList.remove('delivery-tour-target');
  activeTarget = null;
  targetRect.value = null;
};

const updateTargetRect = () => {
  if (!activeTarget) return;

  const rect = activeTarget.getBoundingClientRect();
  targetRect.value = {
    top: rect.top,
    left: rect.left,
    right: rect.right,
    bottom: rect.bottom,
    width: rect.width,
    height: rect.height,
  };
};

const waitForPage = async () => {
  await nextTick();
  await new Promise((resolve) => window.requestAnimationFrame(() => resolve()));
  await new Promise((resolve) => window.setTimeout(resolve, 120));
};

const showAvailableStep = async (direction = 1) => {
  clearTarget();

  while (currentIndex.value >= 0 && currentIndex.value < steps.length) {
    const step = steps[currentIndex.value];

    if (router.currentRoute.value.path !== step.route) {
      await router.push(step.route);
    }

    await waitForPage();
    const target = document.querySelector(step.selector);

    if (target) {
      activeTarget = target;
      activeTarget.classList.add('delivery-tour-target');
      activeTarget.scrollIntoView({ block: 'nearest', inline: 'nearest' });
      updateTargetRect();
      return true;
    }

    currentIndex.value += direction;
  }

  return false;
};

const close = ({ remember = false } = {}) => {
  if (remember) {
    try {
      localStorage.setItem(STORAGE_KEY, 'completed');
    } catch (error) {
      console.warn('사용 가이드 완료 상태를 저장하지 못했습니다.');
    }
  }

  isVisible.value = false;
  clearTarget();
};

const start = async () => {
  currentIndex.value = 0;
  isVisible.value = true;

  if (!await showAvailableStep(1)) {
    close();
  }
};

const move = async (direction) => {
  const nextIndex = currentIndex.value + direction;

  if (nextIndex < 0 || nextIndex >= steps.length) return;

  currentIndex.value = nextIndex;
  if (!await showAvailableStep(direction)) {
    close({ remember: direction > 0 });
  }
};

const finish = () => close({ remember: true });
const skip = () => close({ remember: true });

const handleKeydown = (event) => {
  if (isVisible.value && event.key === 'Escape') {
    skip();
  }
};

onMounted(() => {
  window.addEventListener('resize', updateTargetRect);
  window.addEventListener('scroll', updateTargetRect, true);
  window.addEventListener('keydown', handleKeydown);

  let hasCompleted = false;
  try {
    hasCompleted = localStorage.getItem(STORAGE_KEY) === 'completed';
  } catch (error) {
    hasCompleted = false;
  }

  if (!hasCompleted) {
    start();
  }
});

onBeforeUnmount(() => {
  clearTarget();
  window.removeEventListener('resize', updateTargetRect);
  window.removeEventListener('scroll', updateTargetRect, true);
  window.removeEventListener('keydown', handleKeydown);
});

defineExpose({ start });
</script>

<template>
  <Teleport to="body">
    <div v-if="isVisible" class="guided-tour-layer" aria-live="polite">
      <div class="guided-tour-spotlight" :style="spotlightStyle"></div>
      <section
        class="guided-tour-panel"
        :style="panelStyle"
        role="dialog"
        aria-modal="false"
        aria-labelledby="guided-tour-title"
        aria-describedby="guided-tour-description"
      >
        <div class="guided-tour-progress">
          <span>사용 가이드</span>
          <strong>{{ currentIndex + 1 }} / {{ steps.length }}</strong>
        </div>
        <h2 id="guided-tour-title">{{ currentStep.title }}</h2>
        <p id="guided-tour-description">{{ currentStep.body }}</p>
        <small>Esc 키를 누르면 가이드를 건너뜁니다.</small>
        <div class="guided-tour-actions">
          <button type="button" class="tour-skip" @click="skip">건너뛰기</button>
          <div>
            <button
              type="button"
              class="tour-secondary"
              :disabled="isFirstStep"
              @click="move(-1)"
            >
              이전
            </button>
            <button
              v-if="!isLastStep"
              type="button"
              class="tour-primary"
              @click="move(1)"
            >
              다음
            </button>
            <button
              v-else
              type="button"
              class="tour-primary"
              @click="finish"
            >
              완료
            </button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style>
.guided-tour-layer {
  position: fixed;
  inset: 0;
  z-index: 5000;
  pointer-events: none;
}

.guided-tour-spotlight {
  position: fixed;
  border: 3px solid #38bdf8;
  border-radius: 16px;
  box-shadow: 0 0 0 9999px rgba(15, 23, 42, 0.56), 0 0 0 6px rgba(56, 189, 248, 0.22);
  transition: inset 0.18s ease, width 0.18s ease, height 0.18s ease;
  pointer-events: none;
}

.delivery-tour-target {
  scroll-margin: 20px;
}

.guided-tour-panel {
  position: fixed;
  box-sizing: border-box;
  max-width: calc(100vw - 32px);
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
  padding: 22px;
  border: 1px solid #bae6fd;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 22px 70px rgba(15, 23, 42, 0.28);
  color: #0f172a;
  pointer-events: auto;
}

.guided-tour-progress,
.guided-tour-actions,
.guided-tour-actions > div {
  display: flex;
  align-items: center;
}

.guided-tour-progress,
.guided-tour-actions {
  justify-content: space-between;
  gap: 12px;
}

.guided-tour-progress span,
.guided-tour-progress strong {
  color: #0369a1;
  font-size: 13px;
  font-weight: 900;
}

.guided-tour-panel h2 {
  margin: 14px 0 8px;
  font-size: 24px;
  line-height: 1.25;
}

.guided-tour-panel p {
  margin: 0;
  color: #475569;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.65;
}

.guided-tour-panel small {
  display: block;
  margin-top: 12px;
  color: #64748b;
  font-size: 12px;
}

.guided-tour-actions {
  margin-top: 22px;
}

.guided-tour-actions > div {
  gap: 8px;
}

.guided-tour-actions button {
  min-height: 42px;
  padding: 0 15px;
  border-radius: 11px;
  font: inherit;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}

.guided-tour-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.tour-skip,
.tour-secondary {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
}

.tour-primary {
  border: 1px solid #0284c7;
  background: #0284c7;
  color: #ffffff;
}

@media (max-width: 640px) {
  .guided-tour-panel {
    right: 16px;
    left: 16px !important;
    width: auto !important;
    padding: 18px;
  }

  .guided-tour-panel h2 {
    font-size: 21px;
  }

  .guided-tour-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .guided-tour-actions > div,
  .guided-tour-actions button {
    flex: 1;
  }
}
</style>
