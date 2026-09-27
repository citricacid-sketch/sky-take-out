<template>
  <div class="top-dishes">
    <ul v-if="dishes && dishes.length" class="top-list">
      <li v-for="(d, i) in dishes" :key="d.id || i" class="top-item">
        <span class="rank" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
        <span class="name">{{ d.name }}</span>
        <span class="count">{{ d.sales || 0 }} 份</span>
      </li>
    </ul>
    <div v-else class="top-empty">
      <el-icon :size="24" color="#cbd5e1"><DocumentCopy /></el-icon>
      <p>暂无销售数据</p>
    </div>
  </div>
</template>

<script setup>
import { DocumentCopy } from '@element-plus/icons-vue'
defineProps({
  dishes: { type: Array, default: () => [] },
})
</script>

<style lang="scss" scoped>
.top-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.top-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 6px;
  border-radius: 8px;
  transition: background 0.15s ease;

  &:hover { background: #f8fafc; }

  .rank {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    background: #f1f5f9;
    color: #64748b;
    flex-shrink: 0;

    &.top3 { background: linear-gradient(135deg, #F59E0B, #D97706); color: #fff; }
  }

  .name {
    flex: 1;
    font-size: 14px;
    color: #1f2937;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .count {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
}

.top-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 240px;
  color: #94a3b8;

  p { margin: 0; font-size: 13px; }
}
</style>
