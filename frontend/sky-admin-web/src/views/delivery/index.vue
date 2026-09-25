<template>
  <div class="app-container">
    <div class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.orderId"
          placeholder="订单号"
          clearable
          style="width: 160px"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="配送状态"
          clearable
          style="width: 140px"
        >
          <el-option label="待分配" :value="0" />
          <el-option label="已分配" :value="1" />
          <el-option label="取餐中" :value="2" />
          <el-option label="配送中" :value="3" />
          <el-option label="已完成" :value="4" />
          <el-option label="异常" :value="5" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="配送单号" width="100" />
        <el-table-column prop="orderId" label="订单号" width="100" />
        <el-table-column label="骑手" width="120">
          <template #default="{ row }">
            {{ getRiderName(row.riderId) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="配送状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getDeliveryStatusType(row.status)">
              {{ getDeliveryStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分配时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.assignTime) }}
          </template>
        </el-table-column>
        <el-table-column label="取餐时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.pickupTime) }}
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.finishTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              link
              type="success"
              @click="handleAssign(row)"
            >
              分配骑手
            </el-button>
            <el-button
              v-if="row.status >= 1 && row.status < 4"
              link
              type="warning"
              @click="handleUpdateStatus(row)"
            >
              更新状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 配送详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="配送详情" width="600px">
      <div v-if="deliveryDetail" class="delivery-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="配送单号">{{ deliveryDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ deliveryDetail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="配送状态">
            <el-tag :type="getDeliveryStatusType(deliveryDetail.status)">
              {{ getDeliveryStatusText(deliveryDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="骑手">
            {{ getRiderName(deliveryDetail.riderId) }}
          </el-descriptions-item>
          <el-descriptions-item label="分配时间">{{ formatDateTime(deliveryDetail.assignTime) }}</el-descriptions-item>
          <el-descriptions-item label="取餐时间">{{ formatDateTime(deliveryDetail.pickupTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDateTime(deliveryDetail.finishTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(deliveryDetail.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>配送时间线</el-divider>
        <el-timeline>
          <el-timeline-item
            v-if="deliveryDetail.createTime"
            :timestamp="formatDateTime(deliveryDetail.createTime)"
            type="primary"
          >
            配送单创建
          </el-timeline-item>
          <el-timeline-item
            v-if="deliveryDetail.assignTime"
            :timestamp="formatDateTime(deliveryDetail.assignTime)"
            type="primary"
          >
            分配骑手
          </el-timeline-item>
          <el-timeline-item
            v-if="deliveryDetail.pickupTime"
            :timestamp="formatDateTime(deliveryDetail.pickupTime)"
            type="warning"
          >
            骑手取餐
          </el-timeline-item>
          <el-timeline-item
            v-if="deliveryDetail.finishTime"
            :timestamp="formatDateTime(deliveryDetail.finishTime)"
            type="success"
          >
            配送完成
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <!-- 分配骑手弹窗 -->
    <el-dialog v-model="assignDialogVisible" title="分配骑手" width="400px">
      <el-form label-width="80px">
        <el-form-item label="选择骑手">
          <el-select v-model="selectedRiderId" placeholder="请选择空闲骑手" style="width: 100%">
            <el-option
              v-for="rider in idleRiders"
              :key="rider.id"
              :label="`${rider.name} - ${rider.phone}`"
              :value="rider.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 更新状态弹窗 -->
    <el-dialog v-model="statusDialogVisible" title="更新配送状态" width="400px">
      <el-form label-width="80px">
        <el-form-item label="目标状态">
          <el-select v-model="selectedStatus" placeholder="请选择状态" style="width: 100%">
            <el-option label="已分配" :value="1" />
            <el-option label="取餐中" :value="2" />
            <el-option label="配送中" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="异常" :value="5" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpdateStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getDeliveryPage,
  getDeliveryById,
  assignRider,
  updateDeliveryStatus,
} from '@/api/delivery'
import { getRiderPage } from '@/api/rider'
import {
  formatDateTime,
  getDeliveryStatusText,
  getDeliveryStatusType,
} from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const riderMap = ref({})
const idleRiders = ref([])

const searchForm = reactive({
  orderId: '',
  status: null,
  page: 1,
  pageSize: 10,
})

const detailDialogVisible = ref(false)
const deliveryDetail = ref(null)

const assignDialogVisible = ref(false)
const selectedRiderId = ref(null)
const currentDeliveryId = ref(null)

const statusDialogVisible = ref(false)
const selectedStatus = ref(null)

function getRiderName(riderId) {
  if (!riderId) return '未分配'
  return riderMap.value[riderId]?.name || `骑手${riderId}`
}

async function fetchRiders() {
  try {
    const res = await getRiderPage({ status: 1, page: 1, pageSize: 100 })
    idleRiders.value = res.records
  } catch (e) {
    // ignore
  }
}

async function fetchRiderMap() {
  try {
    const res = await getRiderPage({ page: 1, pageSize: 100 })
    const map = {}
    res.records.forEach((r) => {
      map[r.id] = r
    })
    riderMap.value = map
  } catch (e) {
    // ignore
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (searchForm.orderId) params.orderId = Number(searchForm.orderId)
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    params.page = searchForm.page
    params.pageSize = searchForm.pageSize
    const res = await getDeliveryPage(params)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchForm.page = 1
  fetchData()
}

function handleReset() {
  searchForm.orderId = ''
  searchForm.status = null
  searchForm.page = 1
  fetchData()
}

async function handleDetail(row) {
  deliveryDetail.value = await getDeliveryById(row.id)
  detailDialogVisible.value = true
}

async function handleAssign(row) {
  currentDeliveryId.value = row.id
  selectedRiderId.value = null
  await fetchRiders()
  assignDialogVisible.value = true
}

async function confirmAssign() {
  if (!selectedRiderId.value) {
    ElMessage.warning('请选择骑手')
    return
  }
  await assignRider(currentDeliveryId.value, selectedRiderId.value)
  ElMessage.success('分配成功')
  assignDialogVisible.value = false
  fetchData()
}

async function handleUpdateStatus(row) {
  currentDeliveryId.value = row.id
  selectedStatus.value = null
  statusDialogVisible.value = true
}

async function confirmUpdateStatus() {
  if (selectedStatus.value === null) {
    ElMessage.warning('请选择状态')
    return
  }
  await updateDeliveryStatus(currentDeliveryId.value, selectedStatus.value)
  ElMessage.success('状态更新成功')
  statusDialogVisible.value = false
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchRiderMap()
})
</script>

<style lang="scss" scoped>
.delivery-detail {
  .el-timeline {
    margin-top: 16px;
  }
}
</style>
