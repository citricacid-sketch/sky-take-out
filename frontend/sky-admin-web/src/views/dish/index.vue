<template>
  <div class="app-container">
    <div class="page-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.name"
          placeholder="请输入菜品名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.categoryId"
          placeholder="菜品分类"
          clearable
          style="width: 160px"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="售卖状态"
          clearable
          style="width: 140px"
        >
          <el-option label="启售" :value="1" />
          <el-option label="停售" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增菜品</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="菜品名称" prop="name" min-width="140" />
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              style="width: 50px; height: 50px; border-radius: 4px"
              fit="cover"
              :preview-src-list="[row.image]"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="菜品分类" width="120" />
        <el-table-column label="售价" width="100">
          <template #default="{ row }">
            <span class="text-danger">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售卖状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '停售' : '启售' }}
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
      :title="dialogType === 'add' ? '新增菜品' : '编辑菜品'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜品名称" />
        </el-form-item>
        <el-form-item label="菜品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择菜品分类" style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜品图片" prop="image">
          <div class="upload-wrapper">
            <el-upload
              class="image-uploader"
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="handleUpload"
              accept="image/jpeg,image/png,image/jpg"
            >
              <img v-if="form.image" :src="form.image" class="upload-image" />
              <el-icon v-else class="uploader-icon"><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="菜品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="请输入菜品描述"
          />
        </el-form-item>
        <el-form-item label="口味做法">
          <div class="flavor-section">
            <div
              v-for="(flavor, index) in form.flavors"
              :key="index"
              class="flavor-item"
            >
              <el-input
                v-model="flavor.name"
                placeholder="口味名称（如：辣度）"
                style="width: 140px"
              />
              <el-input
                v-model="flavor.value"
                placeholder="口味值（如：微辣,中辣,特辣）"
                style="flex: 1"
              />
              <el-button :icon="Delete" type="danger" link @click="removeFlavor(index)" />
            </div>
            <el-button :icon="Plus" link type="primary" @click="addFlavor">
              添加口味
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDishPage,
  addDish,
  updateDish,
  deleteDish,
  getDishById,
  updateDishStatus,
  uploadImage,
} from '@/api/dish'
import { getCategoryList } from '@/api/category'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const categoryOptions = ref([])

const searchForm = reactive({
  name: '',
  categoryId: null,
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
  categoryId: null,
  price: 0,
  image: '',
  description: '',
  flavors: [],
})

const formRules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入菜品价格', trigger: 'blur' }],
  image: [{ required: true, message: '请上传菜品图片', trigger: 'change' }],
}

async function fetchCategories() {
  categoryOptions.value = await getCategoryList(1)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getDishPage(searchForm)
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
  searchForm.name = ''
  searchForm.categoryId = null
  searchForm.status = null
  searchForm.page = 1
  fetchData()
}

function resetForm() {
  Object.assign(form, {
    id: null,
    name: '',
    categoryId: null,
    price: 0,
    image: '',
    description: '',
    flavors: [],
  })
}

function handleAdd() {
  resetForm()
  dialogType.value = 'add'
  dialogVisible.value = true
}

async function handleEdit(row) {
  resetForm()
  const data = await getDishById(row.id)
  Object.assign(form, {
    id: data.id,
    name: data.name,
    categoryId: data.categoryId,
    price: data.price,
    image: data.image,
    description: data.description || '',
    flavors: data.flavors?.length ? data.flavors.map((f) => ({ ...f })) : [],
  })
  dialogType.value = 'edit'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  if (dialogType.value === 'add') {
    await addDish(form)
    ElMessage.success('新增成功')
  } else {
    await updateDish(form)
    ElMessage.success('编辑成功')
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除菜品「${row.name}」吗？`, '提示', {
    type: 'warning',
  })
  await deleteDish(String(row.id))
  ElMessage.success('删除成功')
  fetchData()
}

async function handleStatusChange(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateDishStatus(newStatus, row.id)
  ElMessage.success('操作成功')
  fetchData()
}

function beforeImageUpload(file) {
  const isImage = /^image\/(jpeg|png|jpg)$/.test(file.type)
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) ElMessage.error('图片只能是 JPG/PNG 格式!')
  if (!isLt2M) ElMessage.error('图片大小不能超过 2MB!')
  return isImage && isLt2M
}

async function handleUpload(options) {
  const url = await uploadImage(options.file)
  form.image = url
  ElMessage.success('上传成功')
}

function addFlavor() {
  form.flavors.push({ name: '', value: '' })
}

function removeFlavor(index) {
  form.flavors.splice(index, 1)
}

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>

<style lang="scss" scoped>
.image-uploader {
  :deep(.el-upload) {
    border: 1px dashed #dcdfe6;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.2s;
    width: 120px;
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover {
      border-color: #FFC300;
    }
  }

  .upload-image {
    width: 120px;
    height: 120px;
    object-fit: cover;
    display: block;
  }

  .uploader-icon {
    font-size: 28px;
    color: #c0c4cc;
  }
}

.flavor-section {
  width: 100%;

  .flavor-item {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
    align-items: center;
  }
}
</style>
