<template>
  <div class="metric-card" :class="`tone-${color}`">
    <div class="metric-icon">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>
    <div class="metric-body">
      <div class="metric-label">{{ label }}</div>
      <div class="metric-value">{{ value }}</div>
      <div v-if="trend !== undefined && trend !== 0" class="metric-trend" :class="trend > 0 ? 'up' : 'down'">
        <el-icon :size="12"><CaretTop v-if="trend > 0" /><CaretBottom v-else /></el-icon>
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { CaretTop, CaretBottom } from '@element-plus/icons-vue'

defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  icon: { type: [String, Object], required: true },
  color: { type: String, default: 'blue' },
  trend: { type: Number, default: undefined },
})
</script>

<style lang="scss" scoped>
.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06), 0 1px 2px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
  transition: transform 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  }

  .metric-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .metric-body { flex: 1; }

  .metric-label {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 6px;
  }
  .metric-value {
    font-size: 26px;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.1;
  }
  .metric-trend {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    margin-top: 6px;
    font-size: 12px;
    font-weight: 600;
    &.up   { color: #10b981; }
    &.down { color: #ef4444; }
  }

  /* Tones */
  &.tone-warm  .metric-icon { background: #FEF3C7; color: #D97706; }
  &.tone-green .metric-icon { background: #ECFDF5; color: #059669; }
  &.tone-blue  .metric-icon { background: #EFF6FF; color: #3B82F6; }
  &.tone-red   .metric-icon { background: #FEF2F2; color: #DC2626; }
}
</style>
