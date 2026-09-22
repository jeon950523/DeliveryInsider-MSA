<script setup>
defineProps({
  externalStoreId: {
    type: String,
    default: '',
  },
  totalAmount: {
    type: Number,
    default: 0,
  },
  selectedItemCount: {
    type: Number,
    default: 0,
  },
  deliveryAddress: {
    type: String,
    default: '',
  },
  customerRequest: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  'update:deliveryAddress',
  'update:customerRequest',
  'submit',
]);

const formatMoney = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;
</script>

<template>
  <aside class="panel order-composer">
    <div class="panel-heading">
      <div>
        <span class="eyebrow">ORDER</span>
        <h2>고객/플랫폼 이벤트</h2>
      </div>
    </div>

    <dl class="store-summary">
      <div>
        <dt>외부 매장 ID</dt>
        <dd>{{ externalStoreId || '-' }}</dd>
      </div>
      <div>
        <dt>선택 수량</dt>
        <dd>{{ selectedItemCount }}개</dd>
      </div>
    </dl>

    <label class="form-field">
      <span>배달 주소</span>
      <input
        :value="deliveryAddress"
        type="text"
        placeholder="대구광역시 동구 ..."
        :disabled="disabled"
        @input="emit('update:deliveryAddress', $event.target.value)"
      />
    </label>

    <label class="form-field">
      <span>고객 요청사항</span>
      <textarea
        :value="customerRequest"
        rows="4"
        placeholder="예: 문 앞에 놓아주세요."
        :disabled="disabled"
        @input="emit('update:customerRequest', $event.target.value)"
      />
    </label>

    <div class="order-total">
      <span>총 주문금액</span>
      <strong>{{ formatMoney(totalAmount) }}</strong>
    </div>

    <button
      type="button"
      class="primary-button"
      :disabled="disabled || selectedItemCount === 0 || !externalStoreId"
      @click="emit('submit')"
    >
      {{ disabled ? '전송 중...' : '신규 주문 생성' }}
    </button>

    <p class="ownership-note">
      고객이 플랫폼에서 주문하면 플랫폼이 매장/POS로 전달합니다. 이 버튼은 그 외부 주문 발생을 테스트용으로 재현합니다.
      조리 시작·완료는 매장/POS, 픽업·배달 완료는 플랫폼/라이더 동작으로 최근 주문에서 구분합니다.
    </p>
  </aside>
</template>
