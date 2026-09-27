<!--
 * ChartCard 图表卡片容器组件
 * 功能：为图表/数据区域提供统一的卡片外壳（标题 + 内容区域）
 * 特性：
 *   - 支持自定义标题（通过 title 属性或 header 插槽）
 *   - 内容区域通过默认插槽传入（如 echarts 容器 div）
 *   - 注意：父组件 dashboard/index.vue 传入 loading 属性用于控制骨架屏状态，
 *     但当前 ChartCard 本身不渲染 loading，由父级根据 loading 条件决定渲染时机
 * 使用示例：
 *   <ChartCard title="订单分布" :loading="chartLoading">
 *     <div ref="chartRef" class="chart-dom"></div>
 *   </ChartCard>
 */
<template>
  <div class="chart-card">
    <!-- 卡片标题区域：优先使用 header 插槽，否则显示 title 文本 -->
    <div class="chart-card-header" v-if="title || $slots.header">
      <slot name="header">
        <span class="chart-card-title">{{ title }}</span>
      </slot>
    </div>
    <!-- 卡片内容区域：由父组件通过默认插槽填充 -->
    <div class="chart-card-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  /** 卡片标题文本，为空时不显示标题栏 */
  title: { type: String, default: '' },
})
</script>

<style lang="scss" scoped>
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06), 0 1px 2px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);

  .chart-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }
  .chart-card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }
}
</style>
