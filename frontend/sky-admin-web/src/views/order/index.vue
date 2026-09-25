<template>
  <div class="app-container">
    <div class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.number"
          placeholder="订单号"
          clearable
          style="width: 160px"
        />
        <el-input
          v-model="searchForm.phone"
          placeholder="手机号"
          clearable
          style="width: 140px"
        />
        <el-select v-model="searchForm.status" placeholder="订单状态" clearable style="width: 130px">
          <el-option label="待付款" :value="1" />
          <el-option label="待接单" :value="2" />
          <el-option label="已接单" :value="3" />
          <el-option label="派送中" :value="4" />
          <el-option label="已完成" :value="5" />
          <el-option label="已取消" :value="6" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="number" label="订单号" width="180" />
        <el-table-column label="订单详情" prop="orderDishes" min-width="200" show-overflow-tooltip />
        <el-table-column prop="consignee" label="收货人" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.orderTime) }}
          </template>
        </el-table-column>
        <el-table-column label="实收金额" width="100">
          <template #default="{ row }">
            <span class="text-danger">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.status)">
              {{ getOrderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <template v-if="row.actions && row.actions.length">
              <el-button
                v-for="action in row.actions"
                :key="action.action"
                link
                type="primary"
                @click="handleAction(row, action)"
              >
                {{ action.label }}
              </el-button>
            </template>
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

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
      <div v-if="orderDetail" class="order-detail">
        <div class="detail-row">
          <span class="label">订单号：</span>
          <span>{{ orderDetail.number }}</span>
        </div>
        <div class="detail-row">
          <span class="label">下单时间：</span>
          <span>{{ formatDateTime(orderDetail.orderTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="label">订单状态：</span>
          <el-tag :type="getOrderStatusType(orderDetail.status)">
            {{ getOrderStatusText(orderDetail.status) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="label">收货人：</span>
          <span>{{ orderDetail.consignee }}</span>
        </div>
        <div class="detail-row">
          <span class="label">联系电话：</span>
          <span>{{ orderDetail.phone }}</span>
        </div>
        <div class="detail-row">
          <span class="label">收货地址：</span>
          <span>{{ orderDetail.address }}</span>
        </div>
        <div class="detail-row">
          <span class="label">备注：</span>
          <span>{{ orderDetail.remark || '无' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">实收金额：</span>
          <span class="text-danger">¥{{ orderDetail.amount }}</span>
        </div>
        <el-divider>订单菜品</el-divider>
        <el-table :data="orderDetail.orderDetailList" border size="small">
          <el-table-column prop="name" label="菜品名称" />
          <el-table-column label="数量" width="80">
            <template #default="{ row }">{{ row.number }}</template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 拒单/取消原因弹窗 -->
    <el-dialog v-model="reasonDialogVisible" :title="reasonTitle" width="400px">
      <el-input
        v-model="reasonText"
        type="textarea"
        :rows="3"
        :placeholder="`请输入${reasonTitle}原因`"
      />
      <template #footer>
        <el-button @click="reasonDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReasonAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getOrderPage,
  getOrderDetail,
  getOrderActions,
  confirmOrder,
  rejectionOrder,
  cancelOrder,
  deliveryOrder,
  completeOrder,
} from '@/api/order'
import { formatDateTime, getOrderStatusText, getOrderStatusType } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref(null)

const searchForm = reactive({
  number: '',
  phone: '',
  status: null,
  beginTime: '',
  endTime: '',
  page: 1,
  pageSize: 10,
})

const detailDialogVisible = ref(false)
const orderDetail = ref(null)

const reasonDialogVisible = ref(false)
const reasonTitle = ref('')
const reasonText = ref('')
const currentAction = ref(null)
const currentRow = ref(null)

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      searchForm.beginTime = dateRange.value[0]
      searchForm.endTime = dateRange.value[1]
    } else {
      searchForm.beginTime = ''
      searchForm.endTime = ''
    }
    const res = await getOrderPage(searchForm)
    tableData.value = res.records
    total.value = res.total
    // 为每条订单加载可执行动作
    for (const row of tableData.value) {
      try {
        row.actions = await getOrderActions(row.id)
      } catch (e) {
        row.actions = []
      }
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchForm.page = 1
  fetchData()
}

function handleReset() {
  searchForm.number = ''
  searchForm.phone = ''
  searchForm.status = null
  dateRange.value = null
  searchForm.beginTime = ''
  searchForm.endTime = ''
  searchForm.page = 1
  fetchData()
}

async function handleDetail(row) {
  orderDetail.value = await getOrderDetail(row.id)
  detailDialogVisible.value = true
}

function handleAction(row, action) {
  currentRow.value = row
  currentAction.value = action

  if (action.needReason) {
    reasonTitle.value = action.label
    reasonText.value = ''
    reasonDialogVisible.value = true
  } else {
    executeAction(action.action, null)
  }
}

async function confirmReasonAction() {
  if (!reasonText.value.trim()) {
    ElMessage.warning('请输入原因')
    return
  }
  reasonDialogVisible.value = false
  await executeAction(currentAction.value.action, reasonText.value)
}

async function executeAction(actionType, reason) {
  const row = currentRow.value
  try {
    switch (actionType) {
      case 'CONFIRM':
        await confirmOrder({ id: row.id })
        ElMessage.success('接单成功')
        break
      case 'REJECT':
        await rejectionOrder({ id: row.id, rejectionReason: reason })
        ElMessage.success('拒单成功')
        break
      case 'CANCEL':
        await cancelOrder({ id: row.id, cancelReason: reason })
        ElMessage.success('取消成功')
        break
      case 'DELIVER':
        await deliveryOrder(row.id)
        ElMessage.success('派送成功')
        break
      case 'COMPLETE':
        await completeOrder(row.id)
        ElMessage.success('完成订单成功')
        break
      default:
        ElMessage.warning(`未知操作: ${actionType}`)
        return
    }
    fetchData()
  } catch (e) {
    // 错误已在拦截器处理
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.order-detail {
  .detail-row {
    display: flex;
    padding: 6px 0;

    .label {
      width: 90px;
      color: #909399;
      flex-shrink: 0;
    }
  }
}
</style>
