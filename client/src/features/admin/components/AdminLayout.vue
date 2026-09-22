<script setup>
import { RouterView, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../auth/stores/useAuthStore.js';
import AdminSidebar from './AdminSidebar.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const logout = async () => {
  await auth.logout();
  await router.replace('/');
};
</script>

<template>
  <div class="admin-shell">
    <AdminSidebar />
    <section class="admin-main">
      <header class="admin-header">
        <div>
          <span>DeliveryInsider Platform Operations</span>
          <h1>{{ route.meta.title || '관리자' }}</h1>
        </div>
        <button type="button" @click="logout">로그아웃</button>
      </header>
      <main class="admin-content"><RouterView /></main>
    </section>
  </div>
</template>

<style scoped>
.admin-shell { display: flex; min-height: 100dvh; background: #f4f7fb; color: #14213d; }
.admin-main { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.admin-header { min-height: 82px; padding: 16px 28px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #dce5ef; background: #fff; }
.admin-header span { color: #64748b; font-size: 12px; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.admin-header h1 { margin: 4px 0 0; font-size: 24px; }
.admin-header button { border: 1px solid #cbd5e1; border-radius: 10px; padding: 10px 14px; background: #fff; color: #475569; font-weight: 800; cursor: pointer; }
.admin-content { flex: 1; overflow: auto; padding: 28px; }
@media (max-width: 760px) { .admin-shell { display: block; } .admin-content { padding: 18px; } .admin-header { padding: 14px 18px; } }
</style>
