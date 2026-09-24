<template>
  <view class="delivery-page">
    <!-- 地图区域 -->
    <view class="map-container">
      <map
        class="map"
        :longitude="riderLng"
        :latitude="riderLat"
        :markers="markers"
        :polyline="polyline"
        :scale="15"
        v-if="amapKey"
      />
      <view v-else class="map-placeholder">
        <uni-icons type="map" size="60" color="#ddd" />
        <text class="placeholder-text">请先在 config.js 中配置高德地图 Key</text>
        <text class="placeholder-hint">申请地址：https://console.amap.com/dev/key/app</text>
      </view>
    </view>

    <!-- 配送信息面板 -->
    <view class="info-panel">
      <view class="eta-section">
        <text class="eta-label">预计送达</text>
        <text class="eta-time">{{ estimatedTime }}</text>
      </view>

      <!-- 时间线 -->
      <view class="timeline">
        <view
          class="timeline-item"
          v-for="(step, idx) in timelineSteps"
          :key="idx"
        >
          <view class="tl-left">
            <view class="tl-dot" :class="{ active: step.active, done: step.done }" />
            <view v-if="idx < timelineSteps.length - 1" class="tl-line" />
          </view>
          <view class="tl-right">
            <text class="tl-title" :class="{ active: step.active }">{{ step.title }}</text>
            <text class="tl-time">{{ step.time }}</text>
          </view>
        </view>
      </view>

      <!-- 骑手信息 -->
      <view class="rider-section" v-if="riderInfo">
        <image class="rider-avatar" src="https://via.placeholder.com/80x80?text=Rider" mode="aspectFill" />
        <view class="rider-info">
          <text class="rider-name">{{ riderInfo.name || '骑手' }}</text>
          <text class="rider-status">正在为您配送</text>
        </view>
        <view class="rider-call" @click="callRider">
          <uni-icons type="phone" size="20" color="#FFC300" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, onLoad } from 'vue'
import { amapKey } from '../../utils/config'
import request from '../../utils/request'

const orderId = ref(null)
const estimatedTime = ref('--')
const riderLat = ref(39.908823)
const riderLng = ref(116.397470)
const riderInfo = ref(null)
const timelineSteps = ref([])
const polyline = ref([])
const markers = ref([])
let timer = null

const loadDelivery = async () => {
  if (!orderId.value) return
  try {
    const data = await request(`/user/delivery/${orderId.value}`, 'GET')
    if (data) {
      estimatedTime.value = data.estimatedTime || data.estimatedDeliveryTime || '--'
      riderInfo.value = data.rider || null

      // 骑手位置
      if (data.rider && data.rider.lat && data.rider.lng) {
        riderLat.value = data.rider.lat
        riderLng.value = data.rider.lng
      }

      // 时间线
      const steps = data.timeline || data.deliverySteps || buildDefaultTimeline(data.status)
      timelineSteps.value = steps

      // 地图标记
      updateMarkers(data)
    }
  } catch (e) {
    // 使用默认时间线
    timelineSteps.value = buildDefaultTimeline(4)
  }
}

const buildDefaultTimeline = (status) => {
  const steps = [
    { title: '商家已接单', time: '', active: status >= 3, done: status > 3 },
    { title: '骑手已取餐', time: '', active: status >= 4, done: status > 4 },
    { title: '正在配送中', time: '', active: status >= 4, done: status >= 5 },
    { title: '已送达', time: '', active: status >= 5, done: status >= 5 }
  ]
  return steps
}

const updateMarkers = (data) => {
  const marks = []
  // 骑手位置
  if (data.rider) {
    marks.push({
      id: 1,
      latitude: data.rider.lat || riderLat.value,
      longitude: data.rider.lng || riderLng.value,
      iconPath: 'https://via.placeholder.com/40x40?text=Rider',
      width: 40,
      height: 40,
      title: '骑手位置'
    })
  }
  // 商家位置
  if (data.shop) {
    marks.push({
      id: 2,
      latitude: data.shop.lat,
      longitude: data.shop.lng,
      iconPath: 'https://via.placeholder.com/40x40?text=Shop',
      width: 40,
      height: 40,
      title: '商家'
    })
  }
  markers.value = marks

  // 路线
  if (data.path && data.path.length > 0) {
    polyline.value = [{
      points: data.path,
      color: '#FFC300',
      width: 4
    }]
  }
}

const callRider = () => {
  if (riderInfo.value && riderInfo.value.phone) {
    uni.makePhoneCall({ phoneNumber: riderInfo.value.phone })
  } else {
    uni.showToast({ title: '暂无骑手联系方式', icon: 'none' })
  }
}

// 模拟骑手位置移动（在没有真实数据时用于演示）
const simulateRider = () => {
  timer = setInterval(() => {
    riderLat.value += (Math.random() - 0.5) * 0.001
    riderLng.value += (Math.random() - 0.5) * 0.001
  }, 3000)
}

onLoad((options) => {
  if (options && options.orderId) {
    orderId.value = options.orderId
  }
})

onMounted(() => {
  loadDelivery()
  // 演示模式：启动模拟移动
  simulateRider()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.delivery-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.map-container {
  height: 500rpx;
  background-color: #e8e8e8;
}

.map {
  width: 100%;
  height: 100%;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  font-size: 26rpx;
  color: #999;
  margin-top: 20rpx;
}

.placeholder-hint {
  font-size: 22rpx;
  color: #bbb;
  margin-top: 10rpx;
}

.info-panel {
  flex: 1;
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  margin-top: -24rpx;
  padding: 40rpx 30rpx;
  overflow-y: auto;
}

.eta-section {
  text-align: center;
  margin-bottom: 40rpx;
}

.eta-label {
  font-size: 24rpx;
  color: #999;
  display: block;
}

.eta-time {
  font-size: 48rpx;
  font-weight: 700;
  color: #333;
  display: block;
  margin-top: 10rpx;
}

.timeline {
  margin-bottom: 40rpx;
}

.timeline-item {
  display: flex;
}

.tl-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 40rpx;
}

.tl-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background-color: #ddd;

  &.active {
    background-color: #FFC300;
    box-shadow: 0 0 0 6rpx rgba(255, 195, 0, 0.2);
  }

  &.done {
    background-color: #4CAF50;
  }
}

.tl-line {
  width: 2rpx;
  flex: 1;
  background-color: #eee;
  margin: 8rpx 0;
}

.tl-right {
  margin-left: 20rpx;
  padding-bottom: 30rpx;
}

.tl-title {
  font-size: 28rpx;
  color: #666;

  &.active {
    color: #333;
    font-weight: 600;
  }
}

.tl-time {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
  display: block;
}

.rider-section {
  display: flex;
  align-items: center;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 24rpx;
}

.rider-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: #eee;
}

.rider-info {
  flex: 1;
  margin-left: 20rpx;
}

.rider-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.rider-status {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}

.rider-call {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background-color: #FFF8E0;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
