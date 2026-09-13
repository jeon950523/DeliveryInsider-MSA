<script setup>
const links = [
  { to: '/admin', label: '운영 현황' },
  { to: '/admin/users', label: '전체 회원' },
  { to: '/admin/stores', label: '전체 매장' },
  { to: '/admin/platforms', label: '플랫폼 연결' },
  { to: '/admin/subscriptions', label: '전체 구독' },
];
</script>

<template>
  <aside class="admin-sidebar">
    <div class="brand"><strong>DeliveryInsider</strong><span>ADMIN</span></div>
    <p>서비스 전체 운영 상태를 조회합니다.</p>
    <nav>
      <RouterLink v-for="link in links" :key="link.to" :to="link.to">{{ link.label }}</RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.admin-sidebar { width: 250px; min-height: 100dvh; padding: 26px 20px; background: #0f2742; color: #fff; }
.brand { display: flex; align-items: center; gap: 10px; font-size: 18px; }
.brand span { padding: 4px 7px; border-radius: 6px; background: #0ea5e9; font-size: 10px; letter-spacing: .08em; }
p { margin: 12px 0 28px; color: #a9bdd1; font-size: 13px; line-height: 1.5; }
nav { display: grid; gap: 7px; }
a { padding: 13px 14px; border-radius: 10px; color: #cbd8e5; text-decoration: none; font-weight: 750; }
a:hover, a.router-link-exact-active { background: #183b60; color: #fff; }
@media (max-width: 760px) { .admin-sidebar { width: 100%; min-height: auto; padding: 16px; } .admin-sidebar p { display: none; } nav { margin-top: 14px; display: flex; overflow-x: auto; } a { white-space: nowrap; } }
</style>
