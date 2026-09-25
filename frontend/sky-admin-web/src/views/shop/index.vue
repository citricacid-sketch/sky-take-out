<template>
  <div class="app-container">
    <div class="page-card">
      <div class="shop-status-card">
        <div class="status-info">
          <el-icon :size="48" :class="['status-icon', shopStatus === 1 ? 'open' : 'closed']">
            <Shop />
          </el-icon>
          <div class="status-text">
            <h2>{{ shopStatus === 1 ? '营业中' : '打烊中' }}</h2>
            <p class="status-desc">
              {{ shopStatus === 1
                ? '当前店铺处于营业状态，可正常接收订单'
                : '当前店铺处于打烊状态，不再接收新订单' }}
            </p>
          </div>
        </div>
        <el-switch
          v-model="shopStatus"
          :active-value="1"
          :inactive-value="0"
          active-text="营业"
          inactive-text="打烊"
          inline-prompt
          style="--el-switch-on-color: #67c23a; --el-switch-off-color: #909399"
          @change="handleStatusChange"
        />
      </div>

      <el-divider />

      <div class="shop-tips">
        <h3>店铺状态说明</h3>
        <ul>
          <li>营业中：店铺正常接单，顾客可以下单</li>
          <li>打烊中：店铺暂停营业，不再接收新订单</li>
          <li>切换状态后立即生效</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Shop } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShopStatus, setShopStatus } from '@/api/shop'

const shopStatus = ref(0)

async function fetchStatus() {
  shopStatus.value = await getShopStatus()
}

async function handleStatusChange(newStatus) {
  const statusText = newStatus === 1 ? '营业' : '打烊'
  try {
    await ElMessageBox.confirm(`确定要${statusText}吗？`, '提示', {
      type: 'warning',
    })
    await setShopStatus(newStatus)
    ElMessage.success(`已切换为${statusText}状态`)
  } catch (e) {
    // 取消则恢复状态
    shopStatus.value = newStatus === 1 ? 0 : 1
  }
}

onMounted(() => {
  fetchStatus()
})
</script>

<style lang="scss" scoped>
.shop-status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;

  .status-info {
    display: flex;
    align-items: center;
    gap: 20px;

    .status-icon {
      padding: 16px;
      border-radius: 50%;

      &.open {
        color: #67c23a;
        background: #f0f9eb;
      }

      &.closed {
        color: #909399;
        background: #f4f4f5;
      }
    }

    .status-text {
      h2 {
        font-size: 24px;
        margin: 0 0 8px;
        color: #303133;
      }

      .status-desc {
        color: #909399;
        font-size: 14px;
        margin: 0;
      }
    }
  }
}

.shop-tips {
  padding: 0 24px 24px;

  h3 {
    font-size: 16px;
    margin-bottom: 12px;
    color: #303133;
  }

  ul {
    padding-left: 20px;

    li {
      color: #606266;
      padding: 4px 0;
      list-style: disc;
    }
  }
}
</style>
