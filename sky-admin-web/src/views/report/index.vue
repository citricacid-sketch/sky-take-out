<template>
  <div class="app-container">
    <div class="page-card">
      <div class="search-bar">
        <span class="search-label">日期范围：</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="success" :icon="Download" @click="handleExport">导出报表</el-button>
      </div>
    </div>

    <div class="charts-grid">
      <ChartCard title="营业额统计" class="chart-full">
        <div ref="turnoverChartRef" class="chart-dom"></div>
      </ChartCard>

      <ChartCard title="用户统计">
        <div ref="userChartRef" class="chart-dom"></div>
      </ChartCard>

      <ChartCard title="订单统计">
        <div ref="orderChartRef" class="chart-dom"></div>
      </ChartCard>

      <ChartCard title="销量 Top10" class="chart-full">
        <div ref="top10ChartRef" class="chart-dom"></div>
      </ChartCard>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getTurnoverStatistics,
  getUserStatistics,
  getOrdersStatistics,
  getTop10,
  exportReport,
} from '@/api/report'
import ChartCard from '@/components/ChartCard.vue'
import echarts from '@/utils/echarts'

const dateRange = ref(null)
const searchForm = reactive({
  begin: '',
  end: '',
})

const turnoverChartRef = ref(null)
const userChartRef = ref(null)
const orderChartRef = ref(null)
const top10ChartRef = ref(null)

let turnoverChart = null
let userChart = null
let orderChart = null
let top10Chart = null

function initDateRange() {
  const now = new Date()
  const end = formatDate(now)
  const begin = formatDate(new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000))
  dateRange.value = [begin, end]
  searchForm.begin = begin
  searchForm.end = end
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

async function fetchTurnover() {
  const data = await getTurnoverStatistics({ ...searchForm })
  if (!turnoverChartRef.value) return
  turnoverChart = echarts.init(turnoverChartRef.value)
  turnoverChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['营业额'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.dateList?.split(',') || [],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '营业额',
        type: 'line',
        smooth: true,
        data: data.turnoverList?.split(',').map(Number) || [],
        areaStyle: { color: 'rgba(255, 195, 0, 0.1)' },
        itemStyle: { color: '#FFC300' },
      },
    ],
  })
}

async function fetchUser() {
  const data = await getUserStatistics({ ...searchForm })
  if (!userChartRef.value) return
  userChart = echarts.init(userChartRef.value)
  userChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '累计用户'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.dateList?.split(',') || [],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: data.newUserList?.split(',').map(Number) || [],
        itemStyle: { color: '#67c23a' },
      },
      {
        name: '累计用户',
        type: 'line',
        smooth: true,
        data: data.totalUserList?.split(',').map(Number) || [],
        itemStyle: { color: '#409eff' },
      },
    ],
  })
}

async function fetchOrder() {
  const data = await getOrdersStatistics({ ...searchForm })
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  orderChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '有效订单数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.dateList?.split(',') || [],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        data: data.orderCountList?.split(',').map(Number) || [],
        itemStyle: { color: '#409eff' },
      },
      {
        name: '有效订单数',
        type: 'line',
        smooth: true,
        data: data.validOrderCountList?.split(',').map(Number) || [],
        itemStyle: { color: '#67c23a' },
      },
    ],
  })
}

async function fetchTop10() {
  const data = await getTop10({ ...searchForm })
  if (!top10ChartRef.value) return
  top10Chart = echarts.init(top10ChartRef.value)
  const names = data.nameList?.split(',') || []
  const numbers = data.numberList?.split(',').map(Number) || []
  top10Chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: names.reverse(),
    },
    series: [
      {
        type: 'bar',
        data: numbers.reverse(),
        itemStyle: { color: '#FFC300' },
        label: { show: true, position: 'right' },
      },
    ],
  })
}

function disposeAll() {
  if (turnoverChart) { turnoverChart.dispose(); turnoverChart = null }
  if (userChart) { userChart.dispose(); userChart = null }
  if (orderChart) { orderChart.dispose(); orderChart = null }
  if (top10Chart) { top10Chart.dispose(); top10Chart = null }
}

async function fetchAllCharts() {
  await nextTick()
  disposeAll()
  await Promise.all([fetchTurnover(), fetchUser(), fetchOrder(), fetchTop10()])
}

function handleSearch() {
  if (dateRange.value && dateRange.value.length === 2) {
    searchForm.begin = dateRange.value[0]
    searchForm.end = dateRange.value[1]
  }
  fetchAllCharts()
}

function handleReset() {
  initDateRange()
  fetchAllCharts()
}

async function handleExport() {
  try {
    const response = await exportReport({ ...searchForm })
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `报表_${searchForm.begin}_${searchForm.end}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

function handleResize() {
  turnoverChart?.resize()
  userChart?.resize()
  orderChart?.resize()
  top10Chart?.resize()
}

onMounted(() => {
  initDateRange()
  fetchAllCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeAll()
})
</script>

<style lang="scss" scoped>
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;

  .chart-full {
    grid-column: span 2;
  }

  .chart-dom {
    width: 100%;
    height: 350px;
  }

  @media (max-width: 900px) {
    grid-template-columns: 1fr;

    .chart-full {
      grid-column: span 1;
    }
  }
}
</style>
