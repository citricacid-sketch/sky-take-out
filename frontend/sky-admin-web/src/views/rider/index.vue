<template>
  <div class="app-container">
    <div class="page-card">
      <div class="search-bar">
        <el-select
          v-model="searchForm.status"
          placeholder="骑手状态"
          clearable
          style="width: 140px"
        >
          <el-option label="离线" :value="0" />
          <el-option label="空闲" :value="1" />
          <el-option label="配送中" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增骑手</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiderStatusType(row.status)">
              {{ getRiderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="位置" min-width="200">
          <template #default="{ row }">
            <span v-if="row.longitude && row.latitude">
              {{ row.longitude.toFixed(4) }}, {{ row.latitude.toFixed(4) }}
            </span>
            <span v-else class="text-info">未定位</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleUpdateLocation(row)">更新位置</el-button>
            <el-button
              link
              :type="row.status === 0 ? 'success' : 'warning'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 0 ? '上线' : '下线' }}
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增骑手' : '编辑骑手'"
      width="450px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 更新位置弹窗 -->
    <el-dialog v-model="locationDialogVisible" title="更新骑手位置" width="450px">
      <el-form :model="locationForm" label-width="80px">
        <el-form-item label="经度">
          <el-input-number v-model="locationForm.lng" :precision="6" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="locationForm.lat" :precision="6" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpdateLocation">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getRiderPage,
  addRider,
  updateRider,
  updateRiderStatus,
  updateRiderLocation,
} from '@/api/rider'
import { formatDateTime, getRiderStatusText, getRiderStatusType } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const searchForm = reactive({
  status: null,
  page: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  phone: '',
})

const locationDialogVisible = ref(false)
const locationForm = reactive({
  riderId: null,
  lng: 116.404,
  lat: 39.915,
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getRiderPage(searchForm)
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
  searchForm.status = null
  searchForm.page = 1
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: null, name: '', phone: '' })
}

function handleAdd() {
  resetForm()
  dialogType.value = 'add'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogType.value = 'edit'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  if (dialogType.value === 'add') {
    await addRider(form)
    ElMessage.success('新增成功')
  } else {
    await updateRider(form)
    ElMessage.success('编辑成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleStatusChange(row) {
  const newStatus = row.status === 0 ? 1 : 0
  await updateRiderStatus(newStatus, row.id)
  ElMessage.success('操作成功')
  fetchData()
}

function handleUpdateLocation(row) {
  locationForm.riderId = row.id
  locationForm.lng = row.longitude || 116.404
  locationForm.lat = row.latitude || 39.915
  locationDialogVisible.value = true
}

async function confirmUpdateLocation() {
  await updateRiderLocation(locationForm)
  ElMessage.success('位置更新成功')
  locationDialogVisible.value = false
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>
