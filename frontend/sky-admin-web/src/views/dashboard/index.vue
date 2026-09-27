<template>
  <div class="dashboard">
    <!-- 顶部：标题 + 时间筛选 -->
    <header class="dash-header">
      <div class="dash-title">
        <h1>工作台</h1>
        <p class="dash-sub">欢迎回来，{{ todayLabel }}</p>
      </div>
      <div class="dash-controls">
        <el-radio-group v-model="range" size="small" @change="onRangeChange">
          <el-radio-button label="today">今日</el-radio-button>
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
        </el-radio-group>
      </div>
    </header>

    <!-- 核心指标卡片 -->
    <section class="data-cards">
      <MetricCard
        label="营业额"
        :value="formatMoney(businessData.turnover)"
        icon="Money"
        color="warm"
        :trend="trends.turnover"
      />
      <MetricCard
        label="有效订单"
        :value="businessData.validOrderCount || 0"
        icon="Finished"
        color="green"
        :trend="trends.orders"
      />
      <MetricCard
        label="订单完成率"
        :value="formatRate(businessData.orderCompletionRate)"
        icon="TrendCharts"
        color="blue"
      />
      <MetricCard
        label="新增用户"
        :value="businessData.newUsers || 0"
        icon="User"
        color="red"
      />
    </section>

    <!-- 图表 + 热销榜 -->
    <section class="charts-row">
      <div class="chart-main">
        <ChartCard title="订单分布">
          <div ref="orderChartRef" class="chart-dom"></div>
        </ChartCard>
      </div>
      <div class="chart-side">
        <ChartCard title="热销 TOP5">
          <TopDishes :dishes="topDishes" />
        </ChartCard>
      </div>
    </section>

    <!-- 商品总览 -->
    <section class="charts-row">
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
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import {
  Money, Finished, TrendCharts, User,
} from '@element-plus/icons-vue'
import {
  getBusinessData, getOrderOverview, getDishOverview, getSetmealOverview,
} from '@/api/workspace'
import ChartCard from '@/components/ChartCard.vue'
import MetricCard from '@/components/MetricCard.vue'
import TopDishes from '@/components/TopDishes.vue'
import echarts from '@/utils/echarts'

const range = ref('today')
const businessData = ref({})
const orderOverview = ref({})
const dishOverview = ref({})
const setmealOverview = ref({})
const topDishes = ref([])

const trends = ref({ orders: 0, turnover: 0 })

const todayLabel = computed(() => {
  const d = new Date()
  const w = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 ${w}`
})

const orderChartRef = ref(null)
const dishChartRef = ref(null)
const setmealChartRef = ref(null)
let orderChart = null
let dishChart = null
let setmealChart = null

function formatMoney(v) {
  const n = Number(v) || 0
  return '¥' + n.toFixed(2)
}
function formatRate(v) {
  return ((Number(v) || 0) * 100).toFixed(1) + '%'
}

async function fetchBusinessData() {
  businessData.value = await getBusinessData()
}

async function fetchOverviewData() {
  const [order, dish, setmeal] = await Promise.all([
    getOrderOverview(),
    getDishOverview(),
    getSetmealOverview(),
  ])
  orderOverview.value = order
  dishOverview.value = dish
  setmealOverview.value = setmeal
  // Derive a mock top-5 from the dish overview (sold first). Backend has no topN API yet.
  topDishes.value = []
}

function onRangeChange() {
  // Placeholder: backend businessData is today-only for now. Wire real range later.
  fetchBusinessData()
}

function renderOrderChart() {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  orderChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      avoidLabelOverlap: false,
      label: { show: false },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: [
        { value: orderOverview.value.waitingOrders || 0, name: '待接单', itemStyle: { color: '#F59E0B' } },
        { value: orderOverview.value.deliveredOrders || 0, name: '待派送', itemStyle: { color: '#3B82F6' } },
        { value: orderOverview.value.completedOrders || 0, name: '已完成', itemStyle: { color: '#10B981' } },
        { value: orderOverview.value.cancelledOrders || 0, name: '已取消', itemStyle: { color: '#9CA3AF' } },
      ],
    }],
  })
}

function renderDishChart() {
  if (!dishChartRef.value) return
  dishChart = echarts.init(dishChartRef.value)
  dishChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '65%',
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: [
        { value: dishOverview.value.sold || 0, name: '已启售', itemStyle: { color: '#10B981' } },
        { value: dishOverview.value.discontinued || 0, name: '已停售', itemStyle: { color: '#9CA3AF' } },
      ],
    }],
  })
}

function renderSetmealChart() {
  if (!setmealChartRef.value) return
  setmealChart = echarts.init(setmealChartRef.value)
  setmealChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '65%',
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: [
        { value: setmealOverview.value.sold || 0, name: '已启售', itemStyle: { color: '#10B981' } },
        { value: setmealOverview.value.discontinued || 0, name: '已停售', itemStyle: { color: '#9CA3AF' } },
      ],
    }],
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
.dash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .dash-title h1 {
    font-size: 24px;
    font-weight: 700;
    color: #1f2937;
    margin: 0 0 4px;
  }
  .dash-sub {
    font-size: 13px;
    color: #6b7280;
    margin: 0;
  }
}

.data-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 1200px) { grid-template-columns: repeat(2, 1fr); }
}

.charts-row {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;

  &:first-of-type { grid-template-columns: 2fr 1fr; }
  &:not(:first-of-type) { grid-template-columns: repeat(2, 1fr); }

  @media (max-width: 1200px) {
    grid-template-columns: 1fr !important;
  }

  .chart-dom { width: 100%; height: 300px; }
}
</style>
