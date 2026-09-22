<script setup>
defineProps({ columns: { type: Array, required: true }, rows: { type: Array, required: true } });
</script>

<template>
  <div class="table-wrap"><table><thead><tr><th v-for="column in columns" :key="column.key">{{ column.label }}</th></tr></thead><tbody><tr v-for="(row, index) in rows" :key="row.id || row.userId || row.settingId || row.subscriptionId || row.inboxId || `${row.storeId}-${index}`"><td v-for="column in columns" :key="column.key"><slot :name="column.key" :row="row">{{ row[column.key] ?? '-' }}</slot></td></tr></tbody></table></div>
</template>

<style scoped>
.table-wrap { overflow-x: auto; border: 1px solid #dce5ef; border-radius: 14px; background: #fff; }
table { width: 100%; border-collapse: collapse; min-width: 760px; }
th, td { padding: 14px 16px; border-bottom: 1px solid #eef2f7; text-align: left; font-size: 14px; }
th { background: #f8fafc; color: #526273; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
td { color: #334155; }
tbody tr:last-child td { border-bottom: 0; }
</style>
