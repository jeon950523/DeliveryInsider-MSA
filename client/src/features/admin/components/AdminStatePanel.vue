<script setup>
defineProps({ loading: Boolean, error: String, empty: Boolean, emptyText: { type: String, default: '조회된 운영 데이터가 없습니다.' } });
defineEmits(['retry']);
</script>

<template>
  <section v-if="loading" class="state">운영 데이터를 불러오는 중입니다.</section>
  <section v-else-if="error" class="state error" role="alert">
    <strong>조회에 실패했습니다.</strong><span>{{ error }}</span>
    <button type="button" @click="$emit('retry')">다시 조회</button>
  </section>
  <section v-else-if="empty" class="state"><strong>아직 데이터가 없습니다.</strong><span>{{ emptyText }}</span></section>
  <slot v-else />
</template>

<style scoped>
.state { min-height: 190px; display: grid; place-content: center; gap: 8px; text-align: center; border: 1px dashed #cbd5e1; border-radius: 16px; background: #fff; color: #64748b; }
.state strong { color: #334155; }
.state.error { border-color: #fecaca; background: #fff7f7; color: #b42318; }
.state button { justify-self: center; margin-top: 8px; border: 0; border-radius: 9px; padding: 9px 13px; background: #1d4ed8; color: #fff; font-weight: 800; cursor: pointer; }
</style>
