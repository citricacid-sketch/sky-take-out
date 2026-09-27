<!--
 * MetricCard 指标卡片组件
 * 功能：展示单个核心业务指标（如营业额、订单数、完成率、用户数）
 * 特性：
 *   - 四种色调（tone）可选：warm(暖橙)、green(绿)、blue(蓝)、red(红)
 *   - 可选趋势百分比显示（上升/下降箭头 + 百分比）
 *   - 悬停时上浮 + 顶部渐变色条动画
 * 色调说明：
 *   - warm  (暖橙色 #D97706)：用于营业额等收益类指标
 *   - green (绿色   #059669)：用于有效订单等正向指标
 *   - blue  (蓝色   #3B82F6)：用于完成率等中性指标
 *   - red   (红色   #DC2626)：用于新增用户等警示类指标
 */
<template>
  <div class="metric-card" :class="`tone-${color}`">
    <!-- 图标区域：带色调背景的圆角方块 -->
    <div class="metric-icon">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>
    <!-- 内容区域：标签 + 数值 + 趋势 -->
    <div class="metric-body">
      <div class="metric-label">{{ label }}</div>
      <div class="metric-value">{{ value }}</div>
      <!-- 趋势显示：仅在 trend 不为 undefined 且不为 0 时渲染 -->
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
  /** 指标标签，如"营业额""有效订单" */
  label: { type: String, required: true },
  /** 指标数值，支持字符串或数字 */
  value: { type: [String, Number], required: true },
  /** 图标组件名或对象，由父组件传入 */
  icon: { type: [String, Object], required: true },
  /** 色调主题：warm / green / blue / red，默认 blue */
  color: { type: String, default: 'blue' },
  /** 趋势百分比，正数为上升，负数为下降，不传则不显示 */
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
