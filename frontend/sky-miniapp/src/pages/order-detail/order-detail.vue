<template>
  <view class="order-detail-page">
    <!-- 状态横幅 -->
    <view class="status-banner" :style="{ background: statusBg }">
      <view class="status-icon">{{ statusIcon }}</view>
      <view class="status-text">{{ statusText }}</view>
      <view class="status-desc">{{ statusDesc }}</view>
    </view>

    <!-- 配送信息 -->
    <view v-if="deliveryInfo" class="delivery-card card" @click="goDelivery">
      <view class="delivery-top">
        <text class="delivery-icon">🛵</text>
        <text class="delivery-rider">骑手 {{ deliveryInfo.riderName }} 正在配送中</text>
      </view>
      <text class="delivery-arrow">›</text>
    </view>

    <!-- 地址信息 -->
    <view class="address-card card">
      <view class="addr-row">
        <text class="addr-icon">📍</text>
        <view class="addr-info">
          <view class="addr-line">
            <text class="addr-consignee">{{ order.consignee }}</text>
            <text class="addr-phone">{{ order.phone }}</text>
          </view>
          <view class="addr-detail">{{ order.address }}</view>
        </view>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="goods-card card">
      <view class="goods-header">订单商品</view>
      <view v-for="item in order.orderDetailList" :key="item.id" class="goods-item">
        <image class="goods-img" :src="item.image || defaultImage" mode="aspectFill" />
        <view class="goods-info">
          <view class="goods-name text-ellipsis">{{ item.name }}</view>
          <view v-if="item.dishFlavor" class="goods-flavor">{{ item.dishFlavor }}</view>
        </view>
        <view class="goods-right">
          <text class="goods-count">x{{ item.number }}</text>
          <view class="price">
            <text class="symbol">¥</text>
            <text class="value-small">{{ formatPrice(item.amount) }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="info-card card">
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <text class="info-value">{{ order.number }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ formatTime(order.orderTime) }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order.remark || '无' }}</text>
      </view>
    </view>

    <!-- 金额 -->
    <view class="price-card card">
      <view class="price-row total">
        <text>实付金额</text>
        <view class="price">
          <text class="symbol">¥</text>
          <text class="value">{{ formatPrice(order.amount) }}</text>
        </view>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="bottom-actions">
      <view class="btn-action ghost" @click="goChat">联系客服</view>
      <view v-if="order.status === 1" class="btn-action primary" @click="onPay">去支付</view>
      <view v-if="order.status === 1" class="btn-action ghost" @click="onCancel">取消订单</view>
      <view v-if="order.status === 2 || order.status === 3" class="btn-action ghost" @click="onReminder">催单</view>
      <view v-if="order.status === 4" class="btn-action primary" @click="goDelivery">查看配送</view>
      <view v-if="order.status === 5" class="btn-action primary" @click="onRepetition">再来一单</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOrderDetail, cancelOrder, reminderOrder, repetitionOrder, payOrder } from '@/api/order'
import { getDeliveryStatus } from '@/api/other'
import { ORDER_STATUS, formatTime, formatPrice, requireLogin } from '@/utils'

const order = ref({})
const deliveryInfo = ref(null)
const orderId = ref(null)

const statusText = computed(() => {
  return (ORDER_STATUS[order.value.status] && ORDER_STATUS[order.value.status].text) || ''
})

const statusIcon = computed(() => {
  return (ORDER_STATUS[order.value.status] && ORDER_STATUS[order.value.status].icon) || ''
})

const statusBg = computed(() => {
  const s = order.value.status
  if (s === 1) return 'linear-gradient(135deg, #FF6B6B 0%, #FF4757 100%)'
  if (s === 2) return 'linear-gradient(135deg, #FFC300 0%, #FFA500 100%)'
  if (s === 3) return 'linear-gradient(135deg, #10AEFF 0%, #0095FF 100%)'
  if (s === 4) return 'linear-gradient(135deg, #10AEFF 0%, #0095FF 100%)'
  if (s === 5) return 'linear-gradient(135deg, #07C160 0%, #05A050 100%)'
  return 'linear-gradient(135deg, #999 0%, #666 100%)'
})

const statusDesc = computed(() => {
  const s = order.value.status
  if (s === 1) return '请在15分钟内完成支付'
  if (s === 2) return '商家正在准备接单'
  if (s === 3) return '商家已接单，正在备餐'
  if (s === 4) return '骑手正在路上，请耐心等待'
  if (s === 5) return '感谢您的用餐，期待再次光临'
  return ''
})

const defaultImage = '/static/images/default-dish.png'

async function fetchOrder() {
  try {
    order.value = await getOrderDetail(orderId.value)
    if (order.value.status === 4) {
      fetchDelivery()
    }
  } catch (e) {}
}

async function fetchDelivery() {
  try {
    deliveryInfo.value = await getDeliveryStatus(orderId.value)
  } catch (e) {
    deliveryInfo.value = null
  }
}

function goDelivery() {
  uni.navigateTo({ url: `/pages/delivery/delivery?orderId=${orderId.value}` })
}

function goChat() {
  uni.navigateTo({ url: '/pages/chat/chat' })
}

async function onPay() {
  if (!requireLogin()) return
  try {
    await payOrder({ orderNumber: order.value.number, payMethod: 1 })
    uni.showToast({ title: '支付成功', icon: 'success' })
    fetchOrder()
  } catch (e) {}
}

async function onCancel() {
  if (!requireLogin()) return
  uni.showModal({
    title: '提示',
    content: '确定取消该订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(orderId.value)
          uni.showToast({ title: '已取消', icon: 'success' })
          fetchOrder()
        } catch (e) {}
      }
    }
  })
}

async function onReminder() {
  if (!requireLogin()) return
  try {
    await reminderOrder(orderId.value)
    uni.showToast({ title: '已催单', icon: 'success' })
  } catch (e) {}
}

async function onRepetition() {
  if (!requireLogin()) return
  try {
    await repetitionOrder(orderId.value)
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    uni.switchTab({ url: '/pages/index/index' })
  } catch (e) {}
}

onMounted((options) => {
  if (options) orderId.value = options.id
  if (requireLogin() && orderId.value) {
    fetchOrder()
  }
})
</script>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  padding: var(--space-s);
  padding-bottom: 160rpx;
  background: var(--sky-page);
}

.status-banner {
  border-radius: var(--radius-m);
  padding: 48rpx 32rpx;
  text-align: center;
  color: #FFFFFF;
  margin-bottom: var(--space-s);

  .status-icon {
    font-size: 80rpx;
    margin-bottom: 16rpx;
  }

  .status-text {
    font-size: 36rpx;
    font-weight: 700;
  }

  .status-desc {
    font-size: 24rpx;
    opacity: 0.85;
    margin-top: 8rpx;
  }
}

.delivery-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #E8F4FF 0%, #D0EAFF 100%);
  border-left: 8rpx solid var(--sky-info);

  .delivery-top {
    display: flex;
    align-items: center;
    flex: 1;

    .delivery-icon {
      font-size: 36rpx;
      margin-right: 12rpx;
    }

    .delivery-rider {
      font-size: 26rpx;
      color: var(--sky-title);
      font-weight: 600;
    }
  }

  .delivery-arrow {
    font-size: 36rpx;
    color: var(--sky-muted);
  }
}

.address-card {
  .addr-row {
    display: flex;

    .addr-icon {
      font-size: 32rpx;
      margin-right: 16rpx;
      margin-top: 4rpx;
    }

    .addr-info {
      flex: 1;

      .addr-line {
        display: flex;
        align-items: center;

        .addr-consignee {
          font-size: 28rpx;
          font-weight: 600;
          color: var(--sky-title);
        }

        .addr-phone {
          font-size: 24rpx;
          color: var(--sky-muted);
          margin-left: 16rpx;
        }
      }

      .addr-detail {
        font-size: 24rpx;
        color: var(--sky-body);
        margin-top: 6rpx;
        line-height: 1.5;
      }
    }
  }
}

.goods-card {
  .goods-header {
    font-size: 26rpx;
    color: var(--sky-muted);
    margin-bottom: 16rpx;
  }

  .goods-item {
    display: flex;
    align-items: center;
    padding: 16rpx 0;

    .goods-img {
      width: 100rpx;
      height: 100rpx;
      border-radius: var(--radius-s);
    }

    .goods-info {
      flex: 1;
      margin-left: 16rpx;
      min-width: 0;

      .goods-name {
        font-size: 26rpx;
        font-weight: 600;
        color: var(--sky-title);
      }

      .goods-flavor {
        font-size: 20rpx;
        color: var(--sky-muted);
        margin-top: 4rpx;
      }
    }

    .goods-right {
      text-align: right;

      .goods-count {
        font-size: 22rpx;
        color: var(--sky-muted);
      }

      .price {
        margin-top: 4rpx;
      }
    }
  }
}

.info-card {
  .info-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 0;
    font-size: 24rpx;

    .info-label {
      color: var(--sky-muted);
    }

    .info-value {
      color: var(--sky-body);
    }
  }
}

.price-card {
  .price-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 0;
    font-size: 26rpx;
    color: var(--sky-body);

    &.total {
      font-weight: 700;
      color: var(--sky-title);
      font-size: 28rpx;
    }
  }
}

.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  background: var(--sky-card);
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);

  .btn-action {
    padding: 14rpx 32rpx;
    border-radius: 30rpx;
    font-size: 26rpx;
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
</style>
