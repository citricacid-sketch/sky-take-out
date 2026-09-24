<template>
  <div class="dashboard">
    <!-- 数据卡片 -->
    <div class="data-cards">
      <div class="data-card">
        <div class="card-icon" style="background: #fff7e6">
          <el-icon :size="24" color="#e6a23c"><Money /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-label">今日营业额</div>
          <div class="card-value">¥{{ businessData.turnover?.toFixed(2) || '0.00' }}</div>
        </div>
      </div>
      <div class="data-card">
        <div class="card-icon" style="background: #f0f9eb">
          <el-icon :size="24" color="#67c23a"><Finished /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-label">有效订单</div>
          <div class="card-value">{{ businessData.validOrderCount || 0 }}</div>
        </div>
      </div>
      <div class="data-card">
        <div class="card-icon" style="background: #ecf5ff">
          <el-icon :size="24" color="#409eff"><TrendCharts /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-label">订单完成率</div>
          <div class="card-value">{{ (businessData.orderCompletionRate * 100)?.toFixed(1) || '0.0' }}%</div>
        </div>
      </div>
      <div class="data-card">
        <div class="card-icon" style="background: #fef0f0">
          <el-icon :size="24" color="#f56c6c"><User /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-label">新增用户</div>
          <div class="card-value">{{ businessData.newUsers || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-col">
        <ChartCard title="订单概览">
          <div ref="orderChartRef" class="chart-dom"></div>
        </ChartCard>
      </div>
      <div class="chart-col">
        <ChartCard title="菜品总览">
          <div ref="dishChartRef" class="chart-dom"></div>
        </ChartCard>
      </div>
      <div class="chart-col">
        <ChartCard title="套餐总览">
          <div ref="setmealChartRef" class="chart-dom"></div>
        </ChartCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Money, Finished, TrendCharts, User } from '@element-plus/icons-vue'
import {
  getBusinessData,
  getOrderOverview,
  getDishOverview,
  getSetmealOverview,
} from '@/api/workspace'
import ChartCard from '@/components/ChartCard.vue'
import echarts from '@/utils/echarts'

const businessData = ref({})
const orderOverview = ref({})
const dishOverview = ref({})
const setmealOverview = ref({})

const orderChartRef = ref(null)
const dishChartRef = ref(null)
const setmealChartRef = ref(null)

let orderChart = null
let dishChart = null
let setmealChart = null

async function fetchBusinessData() {
  businessData.value = await getBusinessData()
}

async function fetchOverviewData() {
  orderOverview.value = await getOrderOverview()
  dishOverview.value = await getDishOverview()
  setmealOverview.value = await getSetmealOverview()
}

function renderOrderChart() {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  orderChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: [
          { value: orderOverview.value.waitingOrders || 0, name: '待接单', itemStyle: { color: '#e6a23c' } },
          { value: orderOverview.value.deliveredOrders || 0, name: '待派送', itemStyle: { color: '#409eff' } },
          { value: orderOverview.value.completedOrders || 0, name: '已完成', itemStyle: { color: '#67c23a' } },
          { value: orderOverview.value.cancelledOrders || 0, name: '已取消', itemStyle: { color: '#909399' } },
        ],
      },
    ],
  })
}

function renderDishChart() {
  if (!dishChartRef.value) return
  dishChart = echarts.init(dishChartRef.value)
  dishChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: '65%',
        data: [
          { value: dishOverview.value.sold || 0, name: '已启售', itemStyle: { color: '#67c23a' } },
          { value: dishOverview.value.discontinued || 0, name: '已停售', itemStyle: { color: '#909399' } },
        ],
      },
    ],
  })
}

function renderSetmealChart() {
  if (!setmealChartRef.value) return
  setmealChart = echarts.init(setmealChartRef.value)
  setmealChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: '65%',
        data: [
          { value: setmealOverview.value.sold || 0, name: '已启售', itemStyle: { color: '#67c23a' } },
          { value: setmealOverview.value.discontinued || 0, name: '已停售', itemStyle: { color: '#909399' } },
        ],
      },
    ],
  })
}

function handleResize() {
  orderChart?.resize()
  dishChart?.resize()
  setmealChart?.resize()
}

onMounted(async () => {
  await fetchBusinessData()
  await fetchOverviewData()
  await nextTick()
  renderOrderChart()
  renderDishChart()
  renderSetmealChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  orderChart?.dispose()
  dishChart?.dispose()
  setmealChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
  .data-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 16px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .data-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: #fff;
      border-radius: 4px;
      box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);

      .card-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .card-info {
        .card-label {
          font-size: 14px;
          color: #909399;
          margin-bottom: 4px;
        }

        .card-value {
          font-size: 24px;
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }

  .charts-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;

    @media (max-width: 1200px) {
      grid-template-columns: 1fr;
    }

    .chart-dom {
      width: 100%;
      height: 300px;
    }
  }
}
</style>
