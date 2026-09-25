<template>
  <view class="delivery-page">
    <!-- 骑手地图示意 -->
    <view class="map-area">
      <view class="map-placeholder">
        <text class="map-icon">🗺️</text>
        <text class="map-text">配送路线示意图</text>
      </view>
    </view>

    <!-- 配送进度 -->
    <view class="progress-card card">
      <view class="progress-title">配送进度</view>
      <view class="progress-steps">
        <view
          v-for="(step, idx) in steps"
          :key="idx"
          class="step-item"
          :class="{ active: currentStep >= idx, current: currentStep === idx }"
        >
          <view class="step-dot">
            <view v-if="currentStep > idx" class="step-dot-inner done">✓</view>
            <view v-else class="step-dot-inner">{{ idx + 1 }}</view>
          </view>
          <view class="step-line" v-if="idx < steps.length - 1" :class="{ active: currentStep > idx }" />
          <view class="step-content">
            <view class="step-name">{{ step.name }}</view>
            <view class="step-time">{{ step.time || '--' }}</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 骑手信息 -->
    <view v-if="delivery.riderName" class="rider-card card">
      <view class="rider-info">
        <view class="rider-avatar">🛵</view>
        <view class="rider-detail">
          <view class="rider-name">{{ delivery.riderName }}</view>
          <view class="rider-tip">您的骑手正在快马加鞭赶来</view>
        </view>
        <view class="rider-call" @click="callRider">📞</view>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="order-info card">
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <text class="info-value">{{ delivery.orderId }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">配送状态</text>
        <text class="info-value">{{ deliveryStatusText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDeliveryStatus } from '@/api/other'
import { DELIVERY_STATUS, formatTime } from '@/utils'

const delivery = ref({})
const orderId = ref(null)

const currentStep = computed(() => {
  const status = delivery.value.status
  if (status === 0) return 0
  if (status === 1) return 1
  if (status === 2) return 2
  if (status === 3) return 3
  if (status >= 4) return 4
  return 0
})

const deliveryStatusText = computed(() => {
  return DELIVERY_STATUS[delivery.value.status] || '未知'
})

const steps = computed(() => [
  { name: '已分配骑手', time: delivery.value.assignTime ? formatTime(delivery.value.assignTime) : '' },
  { name: '骑手已接单', time: delivery.value.assignTime ? formatTime(delivery.value.assignTime) : '' },
  { name: '骑手取餐中', time: delivery.value.pickupTime ? formatTime(delivery.value.pickupTime) : '' },
  { name: '配送进行中', time: '' },
  { name: '已送达', time: delivery.value.finishTime ? formatTime(delivery.value.finishTime) : '' }
])

function callRider() {
  if (delivery.value.riderPhone) {
    uni.makePhoneCall({ phoneNumber: delivery.value.riderPhone })
  } else {
    uni.showToast({ title: '骑手电话暂未获取', icon: 'none' })
  }
}

onMounted((options) => {
  if (options) orderId.value = options.orderId
  if (orderId.value) {
    getDeliveryStatus(orderId.value).then(data => {
      delivery.value = data || {}
    }).catch(() => {
      delivery.value = {}
    })
  }
})
</script>

<style lang="scss" scoped>
.delivery-page {
  min-height: 100vh;
  background: var(--sky-page);
  padding: var(--space-s);
}

.map-area {
  background: linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%);
  border-radius: var(--radius-m);
  height: 400rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-s);

  .map-placeholder {
    text-align: center;

    .map-icon {
      font-size: 100rpx;
      display: block;
      margin-bottom: 16rpx;
    }

    .map-text {
      font-size: 26rpx;
      color: #666;
    }
  }
}

.progress-card {
  .progress-title {
    font-size: 28rpx;
    font-weight: 700;
    color: var(--sky-title);
    margin-bottom: 32rpx;
  }

  .progress-steps {
    .step-item {
      display: flex;
      align-items: flex-start;
      position: relative;
      padding-bottom: 40rpx;

      .step-dot {
        width: 48rpx;
        height: 48rpx;
        border-radius: 50%;
        background: #E0E0E0;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        z-index: 1;

        .step-dot-inner {
          font-size: 22rpx;
          color: #999;

          &.done {
            color: #FFFFFF;
          }
        }
      }

      .step-line {
        position: absolute;
        left: 23rpx;
        top: 48rpx;
        bottom: 0;
        width: 2rpx;
        background: #E0E0E0;

        &.active {
          background: var(--sky-success);
        }
      }

      .step-content {
        margin-left: 24rpx;

        .step-name {
          font-size: 26rpx;
          color: var(--sky-muted);
        }

        .step-time {
          font-size: 22rpx;
          color: var(--sky-disable);
          margin-top: 4rpx;
        }
      }

      &.active {
        .step-dot {
          background: var(--sky-success);
          .step-dot-inner { color: #FFFFFF; }
        }
      }

      &.current {
        .step-dot {
          background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
          box-shadow: 0 4rpx 12rpx rgba(255, 195, 0, 0.4);
          .step-dot-inner { color: #2B2B2B; font-weight: 700; }
        }
        .step-content .step-name {
          color: var(--sky-title);
          font-weight: 600;
        }
      }
    }
  }
}

.rider-card {
  .rider-info {
    display: flex;
    align-items: center;

    .rider-avatar {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      background: var(--sky-primary-soft);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
    }

    .rider-detail {
      flex: 1;
      margin-left: 20rpx;

      .rider-name {
        font-size: 28rpx;
        font-weight: 600;
        color: var(--sky-title);
      }

      .rider-tip {
        font-size: 22rpx;
        color: var(--sky-muted);
        margin-top: 4rpx;
      }
    }

    .rider-call {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
      background: #E8F8EE;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32rpx;
    }
  }
}

.order-info {
  .info-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 0;
    font-size: 24rpx;

    .info-label { color: var(--sky-muted); }
    .info-value { color: var(--sky-body); }
  }
}
</style>
