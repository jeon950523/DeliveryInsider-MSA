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
        <h2>주문 만들기</h2>
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
      {{ disabled ? '전송 중...' : '주문하기' }}
    </button>

    <p class="ownership-note">
      이 화면은 CREATED 주문 생성과 고객 취소·기사 픽업·배달 완료만 제어합니다.
      조리 상태는 DeliveryInsider 점주 화면이 소유합니다.
    </p>
  </aside>
</template>
