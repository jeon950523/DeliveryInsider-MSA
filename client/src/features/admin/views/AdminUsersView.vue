<script setup>
import { onMounted } from 'vue';
import { useAdminStore } from '../stores/useAdminStore.js';
import AdminStatePanel from '../components/AdminStatePanel.vue';
import AdminTable from '../components/AdminTable.vue';
const admin = useAdminStore(); const state = admin.resources.users;
const columns = [{ key: 'userId', label: '회원 ID' }, { key: 'email', label: '이메일' }, { key: 'role', label: '역할' }, { key: 'phoneVerified', label: '휴대폰 인증' }, { key: 'status', label: '상태' }, { key: 'createdAt', label: '가입일' }];
const formatDate = value => value ? new Date(value).toLocaleString('ko-KR') : '-';
onMounted(() => admin.loadUsers().catch(() => {}));
</script>
<template><div class="page"><p>비밀번호·토큰·전화번호는 노출하지 않습니다.</p><AdminStatePanel :loading="state.loading" :error="state.error" :empty="state.data?.items?.length === 0" empty-text="가입한 회원이 없습니다." @retry="admin.loadUsers().catch(() => {})"><AdminTable :columns="columns" :rows="state.data?.items || []"><template #phoneVerified="{ row }">{{ row.phoneVerified ? '완료' : '미완료' }}</template><template #createdAt="{ row }">{{ formatDate(row.createdAt) }}</template></AdminTable></AdminStatePanel></div></template>
<style scoped>.page>p{margin:0 0 16px;color:#64748b}</style>
