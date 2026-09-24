<template>
  <view class="order-detail-page" v-if="order">
    <!-- 状态区 -->
    <view class="status-section">
      <text class="status-title">{{ statusTitle }}</text>
      <text class="status-desc">{{ statusDesc }}</text>
    </view>

    <!-- 配送信息 -->
    <view v-if="order.status >= 4" class="delivery-section card" @click="goDelivery">
      <view class="delivery-info">
        <text class="delivery-title">配送中</text>
        <text class="delivery-eta">预计 {{ order.estimatedDeliveryTime || '30分钟' }} 送达</text>
      </view>
      <uni-icons type="right" size="16" color="#999" />
    </view>

    <!-- 地址 -->
    <view class="address-section card">
      <view class="addr-row">
        <uni-icons type="location" size="18" color="#999" />
        <view class="addr-content">
          <text class="addr-user">{{ order.consignee }} {{ order.phone }}</text>
          <text class="addr-text">{{ order.address }}</text>
        </view>
      </view>
    </view>

    <!-- 菜品 -->
    <view class="dishes-section card">
      <view class="dish-row" v-for="(item, idx) in order.orderDetailList" :key="idx">
        <image class="dish-img" :src="item.image || 'https://via.placeholder.com/90x90?text=Dish'" mode="aspectFill" />
        <view class="dish-info">
          <text class="dish-name">{{ item.name }}</text>
          <text v-if="item.dishFlavor" class="dish-flavor">{{ item.dishFlavor }}</text>
        </view>
        <text class="dish-qty">x{{ item.number }}</text>
        <text class="dish-price">¥{{ item.amount }}</text>
      </view>
    </view>

    <!-- 金额明细 -->
    <view class="amount-section card">
      <view class="amount-row">
        <text>商品金额</text>
        <text>¥{{ (order.amount - order.packingFee - order.deliveryFee).toFixed(2) }}</text>
      </view>
      <view class="amount-row">
        <text>打包费</text>
        <text>¥{{ order.packingFee }}</text>
      </view>
      <view class="amount-row">
        <text>配送费</text>
        <text>¥{{ order.deliveryFee }}</text>
      </view>
      <view class="amount-row total-row">
        <text class="total-label">实付</text>
        <text class="total-amount">¥{{ order.amount }}</text>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="info-section card">
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <text class="info-value">{{ order.number }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ order.orderTime || order.createTime }}</text>
      </view>
      <view v-if="order.remark" class="info-row">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order.remark }}</text>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="actions-bar" v-if="actions.length">
      <view
        v-for="action in actions"
        :key="action.action"
        class="action-btn"
        :class="action.action === 'PAY' ? 'btn-primary-action' : 'btn-secondary-action'"
        @click="handleAction(action)"
      >
        {{ action.label }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onLoad } from 'vue'
import request from '../../utils/request'

const order = ref(null)
const actions = ref([])
const orderId = ref(null)

const statusMap = {
  1: { title: '待付款', desc: '请尽快完成支付' },
  2: { title: '待接单', desc: '商家正在准备接单' },
  3: { title: '已接单', desc: '商家已接单，正在准备' },
  4: { title: '派送中', desc: '骑手正在配送中' },
  5: { title: '已完成', desc: '订单已完成' },
  6: { title: '已取消', desc: '订单已取消' }
}

const statusTitle = computed(() => statusMap[order.value?.status]?.title || '')
const statusDesc = computed(() => statusMap[order.value?.status]?.desc || '')

const loadOrder = async () => {
  try {
    order.value = await request(`/user/order/orderDetail/${orderId.value}`, 'GET')
  } catch (e) {}
}

const loadActions = async () => {
  try {
    const data = await request(`/user/order/${orderId.value}/actions`, 'GET')
    actions.value = data || []
  } catch (e) {}
}

const goDelivery = () => {
  uni.navigateTo({ url: `/pages/delivery/index?orderId=${orderId.value}` })
}

const handleAction = async (action) => {
  if (action.action === 'PAY') {
    try {
      await request('/user/order/payment', 'PUT', { orderNumber: order.value.number })
      uni.showToast({ title: '支付成功', icon: 'success' })
      loadOrder()
      loadActions()
    } catch (e) {}
  } else if (action.action === 'CANCEL') {
    if (action.needReason) {
      uni.showModal({
        title: '取消订单',
        editable: true,
        placeholderText: '请输入取消原因',
        success: async (res) => {
          if (res.confirm) {
            await request(`/user/order/cancel/${orderId.value}`, 'PUT', { reason: res.content || '' })
            uni.showToast({ title: '已取消', icon: 'success' })
            loadOrder()
            loadActions()
          }
        }
      })
    } else {
      uni.showModal({
        title: '提示',
        content: '确定取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            await request(`/user/order/cancel/${orderId.value}`, 'PUT')
            uni.showToast({ title: '已取消', icon: 'success' })
            loadOrder()
            loadActions()
          }
        }
      })
    }
  } else if (action.action === 'CONFIRM') {
    // 后端无 PUT /user/order/complete/${id} 用户端接口，完成订单为管理员操作
    uni.showToast({ title: '确认收货请联系商家或等待自动完成', icon: 'none' })
  } else if (action.action === 'REPETITION') {
    try {
      await request(`/user/order/repetition/${orderId.value}`, 'POST')
      uni.showToast({ title: '已加入购物车', icon: 'success' })
    } catch (e) {}
  } else if (action.action === 'REMIND') {
    try {
      await request(`/user/order/reminder/${orderId.value}`, 'GET')
      uni.showToast({ title: '已催单', icon: 'success' })
    } catch (e) {}
  }
}

onLoad((options) => {
  orderId.value = options.id
  loadOrder()
  loadActions()
})
</script>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.status-section {
  background: linear-gradient(180deg, #FFC300 0%, #FFD54F 100%);
  padding: 50rpx 40rpx;
  margin: -20rpx -20rpx 20rpx;
}

.status-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
  display: block;
}

.status-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 10rpx;
  display: block;
}

.delivery-section {
  display: flex;
  align-items: center;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.delivery-info {
  flex: 1;
}

.delivery-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: block;
}

.delivery-eta {
  font-size: 24rpx;
  color: #666;
  margin-top: 8rpx;
  display: block;
}

.address-section {
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.addr-row {
  display: flex;
}

.addr-content {
  margin-left: 16rpx;
}

.addr-user {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.addr-text {
  font-size: 26rpx;
  color: #666;
  margin-top: 8rpx;
}

.dishes-section {
  padding: 10rpx 30rpx;
  margin-bottom: 20rpx;
}

.dish-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
}

.dish-img {
  width: 90rpx;
  height: 90rpx;
  border-radius: 8rpx;
}

.dish-info {
  flex: 1;
  margin-left: 20rpx;
}

.dish-name {
  font-size: 28rpx;
  color: #333;
}

.dish-flavor {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.dish-qty {
  font-size: 26rpx;
  color: #666;
  margin-right: 20rpx;
}

.dish-price {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.amount-section {
  padding: 10rpx 30rpx;
  margin-bottom: 20rpx;
}

.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: #666;
}

.total-row {
  border-top: 1rpx solid #f5f5f5;
  padding-top: 20rpx;
  margin-top: 10rpx;
}

.total-label {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.total-amount {
  font-size: 32rpx;
  font-weight: 600;
  color: #FF4B33;
}

.info-section {
  padding: 10rpx 30rpx;
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
}

.info-label {
  font-size: 26rpx;
  color: #999;
}

.info-value {
  font-size: 26rpx;
  color: #333;
}

.actions-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  border-top: 1rpx solid #eee;
}

.action-btn {
  padding: 16rpx 32rpx;
  border-radius: 50rpx;
  font-size: 26rpx;
}

.btn-primary-action {
  background-color: #FFC300;
  color: #fff;
}

.btn-secondary-action {
  background-color: #fff;
  color: #666;
  border: 1rpx solid #ddd;
}
</style>
