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
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06), 0 1px 2px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--tone-color, #3B82F6), transparent);
    opacity: 0;
    transition: opacity 0.25s ease;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);

    &::before { opacity: 1; }
  }

  .metric-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    transition: transform 0.25s ease;
  }

  &:hover .metric-icon {
    transform: scale(1.05);
  }

  .metric-body { flex: 1; min-width: 0; }

  .metric-label {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 6px;
    font-weight: 500;
  }
  .metric-value {
    font-size: 28px;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.1;
    letter-spacing: -0.5px;
  }
  .metric-trend {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    margin-top: 8px;
    font-size: 12px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 12px;
    &.up {
      color: #059669;
      background: #ECFDF5;
    }
    &.down {
      color: #DC2626;
      background: #FEF2F2;
    }
  }

  /* Tones */
  &.tone-warm  { --tone-color: #D97706; }
  &.tone-warm  .metric-icon { background: #FEF3C7; color: #D97706; }
  &.tone-green { --tone-color: #059669; }
  &.tone-green .metric-icon { background: #ECFDF5; color: #059669; }
  &.tone-blue  { --tone-color: #3B82F6; }
  &.tone-blue  .metric-icon { background: #EFF6FF; color: #3B82F6; }
  &.tone-red   { --tone-color: #DC2626; }
  &.tone-red   .metric-icon { background: #FEF2F2; color: #DC2626; }
}
</style>
