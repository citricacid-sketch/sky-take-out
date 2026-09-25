<template>
  <view class="order-list-page">
    <!-- 状态筛选 -->
    <view class="filter-bar">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="filter-item"
        :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 订单列表 -->
    <scroll-view scroll-y class="order-list" @scrolltolower="loadMore">
      <view
        v-for="order in orders"
        :key="order.id"
        class="order-card card"
        @click="goDetail(order.id)"
      >
        <!-- 头部 -->
        <view class="order-header">
          <view class="order-id">订单号：{{ order.number }}</view>
          <view class="order-status" :style="{ color: statusColor(order.status) }">
            {{ statusText(order.status) }}
          </view>
        </view>

        <!-- 菜品 -->
        <view class="order-dishes">
          <view class="dish-names">{{ order.orderDishes }}</view>
        </view>

        <!-- 底部 -->
        <view class="order-footer">
          <view class="order-time">{{ formatTime(order.orderTime) }}</view>
          <view class="price">
            <text class="symbol">¥</text>
            <text class="value">{{ formatPrice(order.amount) }}</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="order-actions">
          <view v-if="order.status === 1" class="btn-action primary" @click.stop="onPay(order)">去支付</view>
          <view v-if="order.status === 1" class="btn-action ghost" @click.stop="onCancel(order)">取消</view>
          <view v-if="order.status === 2 || order.status === 3" class="btn-action ghost" @click.stop="onReminder(order)">催单</view>
          <view v-if="order.status === 4" class="btn-action primary" @click.stop="goDelivery(order)">查看配送</view>
          <view v-if="order.status === 5" class="btn-action primary" @click.stop="onRepetition(order)">再来一单</view>
          <view v-if="order.status === 5" class="btn-action ghost" @click.stop="goChat">评价</view>
        </view>
      </view>

      <view v-if="orders.length === 0 && !loading" class="empty-state">
        <view class="empty-icon">📋</view>
        <view class="empty-text">暂无相关订单</view>
        <view class="empty-action">
          <view class="btn-primary" @click="goHome">去点餐</view>
        </view>
      </view>

      <view v-if="loading" class="loading-tip">加载中...</view>
      <view v-if="noMore && orders.length > 0" class="loading-tip">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHistoryOrders, cancelOrder, reminderOrder, repetitionOrder, payOrder } from '@/api/order'
import { ORDER_STATUS, formatTime, formatPrice, requireLogin } from '@/utils'

const tabs = [
  { label: '全部', value: undefined },
  { label: '待付款', value: 1 },
  { label: '待接单', value: 2 },
  { label: '派送中', value: 4 },
  { label: '已完成', value: 5 }
]

const activeTab = ref(undefined)
const orders = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const noMore = computed(() => orders.value.length >= total.value)

function statusText(status) {
  return (ORDER_STATUS[status] && ORDER_STATUS[status].text) || '未知'
}

function statusColor(status) {
  return (ORDER_STATUS[status] && ORDER_STATUS[status].color) || '#999'
}

function switchTab(val) {
  activeTab.value = val
  page.value = 1
  orders.value = []
  fetchOrders()
}

async function fetchOrders() {
  if (loading.value) return
  loading.value = true
  try {
    const data = await getHistoryOrders({
      page: page.value,
      pageSize,
      status: activeTab.value
    })
    orders.value = page.value === 1 ? data.records : [...orders.value, ...data.records]
    total.value = data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (noMore.value || loading.value) return
  page.value++
  fetchOrders()
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function goDelivery(order) {
  uni.navigateTo({ url: `/pages/delivery/delivery?orderId=${order.id}` })
}

function goChat() {
  uni.navigateTo({ url: '/pages/chat/chat' })
}

async function onPay(order) {
  if (!requireLogin()) return
  try {
    await payOrder({ orderNumber: order.number, payMethod: 1 })
    uni.showToast({ title: '支付成功', icon: 'success' })
    switchTab(activeTab.value)
  } catch (e) {}
}

async function onCancel(order) {
  if (!requireLogin()) return
  uni.showModal({
    title: '提示',
    content: '确定取消该订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(order.id)
          uni.showToast({ title: '已取消', icon: 'success' })
          switchTab(activeTab.value)
        } catch (e) {}
      }
    }
  })
}

async function onReminder(order) {
  if (!requireLogin()) return
  try {
    await reminderOrder(order.id)
    uni.showToast({ title: '已催单，商家会尽快处理', icon: 'success' })
  } catch (e) {}
}

async function onRepetition(order) {
  if (!requireLogin()) return
  try {
    await repetitionOrder(order.id)
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    uni.switchTab({ url: '/pages/index/index' })
  } catch (e) {}
}

onMounted(() => {
  if (!requireLogin()) return
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background: var(--sky-page);
}

.filter-bar {
  display: flex;
  background: var(--sky-card);
  padding: 0 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .filter-item {
    flex: 1;
    text-align: center;
    padding: 28rpx 0;
    font-size: 26rpx;
    color: var(--sky-muted);
    position: relative;
    transition: color 0.2s;

    &.active {
      color: var(--sky-title);
      font-weight: 700;

      &::after {
        content: '';
        position: absolute;
        bottom: 8rpx;
        left: 50%;
        transform: translateX(-50%);
        width: 40rpx;
        height: 6rpx;
        background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
        border-radius: 3rpx;
      }
    }
  }
}

.order-list {
  padding: var(--space-s);
  height: calc(100vh - 90rpx);
}

.order-card {
  margin-bottom: var(--space-s);

  .order-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .order-id {
      font-size: 24rpx;
      color: var(--sky-muted);
    }

    .order-status {
      font-size: 26rpx;
      font-weight: 600;
    }
  }

  .order-dishes {
    margin: 16rpx 0;

    .dish-names {
      font-size: 26rpx;
      color: var(--sky-body);
      line-height: 1.5;
    }
  }

  .order-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 16rpx;
    border-bottom: 1rpx solid var(--sky-border);

    .order-time {
      font-size: 22rpx;
      color: var(--sky-muted);
    }
  }

  .order-actions {
    display: flex;
    justify-content: flex-end;
    padding-top: 16rpx;

    .btn-action {
      padding: 12rpx 28rpx;
      border-radius: 30rpx;
      font-size: 24rpx;
      margin-left: 16rpx;

      &.primary {
        background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
        color: #2B2B2B;
        font-weight: 600;
      }

      &.ghost {
        background: var(--sky-card);
        color: var(--sky-body);
        border: 2rpx solid var(--sky-border);
      }
    }
  }
}

.empty-state {
  padding: 160rpx 40rpx;

  .empty-action {
    width: 300rpx;
    margin: 40rpx auto 0;
  }
}

.loading-tip {
  text-align: center;
  font-size: 24rpx;
  color: var(--sky-muted);
  padding: 32rpx;
}
</style>
