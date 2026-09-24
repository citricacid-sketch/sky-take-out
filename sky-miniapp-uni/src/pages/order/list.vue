<template>
  <view class="order-list-page">
    <!-- Tab 筛选 -->
    <view class="tabs">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'all' }"
        @click="activeTab = 'all'"
      >全部</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'unpaid' }"
        @click="activeTab = 'unpaid'"
      >待付款</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'processing' }"
        @click="activeTab = 'processing'"
      >待接单</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'delivering' }"
        @click="activeTab = 'delivering'"
      >派送中</view>
    </view>

    <!-- 订单列表 -->
    <scroll-view scroll-y class="order-scroll">
      <view v-if="filteredOrders.length === 0" class="empty-state">
        <uni-icons type="calendar" size="80" color="#ddd" />
        <text class="empty-text">暂无订单</text>
      </view>
      <order-card
        v-for="order in filteredOrders"
        :key="order.id"
        :order="order"
        @click="goDetail"
        @action-click="handleAction"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import OrderCard from '../../components/order-card.vue'
import request from '../../utils/request'

const activeTab = ref('all')
const orders = ref([])

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  if (activeTab.value === 'unpaid') return orders.value.filter(o => o.status === 1)
  if (activeTab.value === 'processing') return orders.value.filter(o => [2, 3].includes(o.status))
  if (activeTab.value === 'delivering') return orders.value.filter(o => o.status === 4)
  return orders.value
})

const loadOrders = async () => {
  try {
    const data = await request('/user/order/historyOrders', 'GET')
    orders.value = data || []
  } catch (e) {
    orders.value = []
  }
}

const goDetail = (order) => {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

const handleAction = async ({ order, action }) => {
  if (action.action === 'PAY') {
    // 跳转支付
    await handlePay(order)
  } else if (action.action === 'CANCEL') {
    await handleCancel(order, action)
  } else if (action.action === 'CONFIRM') {
    // 后端无 PUT /user/order/complete/${id} 用户端接口，完成订单为管理员操作
    uni.showToast({ title: '确认收货请联系商家或等待自动完成', icon: 'none' })
  } else if (action.action === 'REPETITION') {
    await handleRepetition(order)
  } else if (action.action === 'REMIND') {
    await handleReminder(order)
  }
}

const handlePay = async (order) => {
  try {
    await request('/user/order/payment', 'PUT', { orderNumber: order.number })
    uni.showToast({ title: '支付成功', icon: 'success' })
    loadOrders()
  } catch (e) {}
}

const handleCancel = async (order, action) => {
  if (action.needReason) {
    uni.showModal({
      title: '取消订单',
      editable: true,
      placeholderText: '请输入取消原因',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request(`/user/order/cancel/${order.id}`, 'PUT', { reason: res.content || '' })
            uni.showToast({ title: '已取消', icon: 'success' })
            loadOrders()
          } catch (e) {}
        }
      }
    })
  } else {
    uni.showModal({
      title: '提示',
      content: '确定取消该订单吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await request(`/user/order/cancel/${order.id}`, 'PUT')
            uni.showToast({ title: '已取消', icon: 'success' })
            loadOrders()
          } catch (e) {}
        }
      }
    })
  }
}

const handleConfirm = async (order) => {
  // 后端无 PUT /user/order/complete/${id} 用户端接口，完成订单为管理员操作
  /* try {
    await request(`/user/order/complete/${order.id}`, 'PUT')
    uni.showToast({ title: '已确认收货', icon: 'success' })
    loadOrders()
  } catch (e) {} */
}

const handleRepetition = async (order) => {
  try {
    await request(`/user/order/repetition/${order.id}`, 'POST')
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) {}
}

const handleReminder = async (order) => {
  try {
    await request(`/user/order/reminder/${order.id}`, 'GET')
    uni.showToast({ title: '已催单', icon: 'success' })
  } catch (e) {}
}

onMounted(() => {
  loadOrders()
})
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.tabs {
  display: flex;
  background-color: #fff;
  padding: 0 30rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;

  &.active {
    color: #FFC300;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 4rpx;
      background-color: #FFC300;
      border-radius: 2rpx;
    }
  }
}

.order-scroll {
  height: calc(100vh - 88rpx);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-top: 30rpx;
}
</style>
