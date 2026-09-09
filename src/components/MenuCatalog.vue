<script setup>
defineProps({
  menus: {
    type: Array,
    default: () => [],
  },
  quantities: {
    type: Object,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['change-quantity']);

const formatMoney = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;
</script>

<template>
  <section class="panel menu-panel">
    <div class="panel-heading">
      <div>
        <span class="eyebrow">EXTERNAL MENU CATALOG</span>
        <h2>메뉴 선택</h2>
      </div>
      <small>외부 플랫폼 DB 기준</small>
    </div>

    <div v-if="menus.length" class="menu-grid">
      <article
        v-for="menu in menus"
        :key="menu.externalMenuId"
        class="menu-card"
      >
        <div class="menu-card__meta">
          <span>{{ menu.catalogKey }}</span>
          <small>{{ menu.externalMenuId }}</small>
        </div>
        <h3>{{ menu.menuName }}</h3>
        <strong>{{ formatMoney(menu.price) }}</strong>

        <div class="quantity-control">
          <button
            type="button"
            :disabled="disabled || !quantities[menu.externalMenuId]"
            @click="emit('change-quantity', menu.externalMenuId, -1)"
          >
            −
          </button>
          <b>{{ quantities[menu.externalMenuId] || 0 }}</b>
          <button
            type="button"
            :disabled="disabled"
            @click="emit('change-quantity', menu.externalMenuId, 1)"
          >
            +
          </button>
        </div>
      </article>
    </div>

    <div v-else class="empty-state">
      현재 선택한 플랫폼에 주문 가능한 메뉴가 없습니다.
    </div>
  </section>
</template>
