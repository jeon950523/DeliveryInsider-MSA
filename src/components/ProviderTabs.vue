<script setup>
import { PROVIDERS } from '../constants/providers.js';

const props = defineProps({
  modelValue: {
    type: String,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:modelValue']);

const selectProvider = (platformType) => {
  if (props.disabled || props.modelValue === platformType) {
    return;
  }

  emit('update:modelValue', platformType);
};
</script>

<template>
  <nav class="provider-tabs" aria-label="외부 배달 플랫폼 선택">
    <button
      v-for="provider in PROVIDERS"
      :key="provider.type"
      type="button"
      class="provider-tab"
      :class="{ 'is-active': modelValue === provider.type }"
      :disabled="disabled"
      @click="selectProvider(provider.type)"
    >
      <span class="provider-tab__name">{{ provider.label }}</span>
      <small>{{ provider.type }}</small>
    </button>
  </nav>
</template>
