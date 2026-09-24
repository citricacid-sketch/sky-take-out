<template>
  <view class="profile-page">
    <!-- 用户信息 -->
    <view class="user-section">
      <view class="user-info">
        <view class="user-avatar">
          <uni-icons type="person" size="36" color="#fff" />
        </view>
        <view class="user-detail">
          <text class="user-name">{{ userStore.isLogin ? '微信用户' : '未登录' }}</text>
          <text class="user-id" v-if="userStore.isLogin">ID: {{ userStore.userId }}</text>
        </view>
      </view>
      <view v-if="!userStore.isLogin" class="login-btn" @click="goLogin">立即登录</view>
    </view>

    <!-- 订单入口 -->
    <view class="order-entry card">
      <view class="order-entry-header" @click="goOrders">
        <text>我的订单</text>
        <uni-icons type="right" size="16" color="#999" />
      </view>
      <view class="order-entry-grid">
        <view class="grid-item" @click="goOrdersByStatus(1)">
          <view class="grid-icon">
            <uni-icons type="wallet" size="24" color="#FF8C00" />
            <view v-if="unpaidCount > 0" class="grid-badge">{{ unpaidCount }}</view>
          </view>
          <text class="grid-label">待付款</text>
        </view>
        <view class="grid-item" @click="goOrdersByStatus(2)">
          <view class="grid-icon">
            <uni-icons type="shop" size="24" color="#1890ff" />
          </view>
          <text class="grid-label">待接单</text>
        </view>
        <view class="grid-item" @click="goOrdersByStatus(4)">
          <view class="grid-icon">
            <uni-icons type="paperplane" size="24" color="#7B61FF" />
          </view>
          <text class="grid-label">派送中</text>
        </view>
        <view class="grid-item" @click="goOrdersByStatus(5)">
          <view class="grid-icon">
            <uni-icons type="checkmarkempty" size="24" color="#52c41a" />
          </view>
          <text class="grid-label">已完成</text>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-list card">
      <view class="menu-row" @click="goAddress">
        <view class="menu-left">
          <uni-icons type="location" size="20" color="#FF6B6B" />
          <text>收货地址</text>
        </view>
        <uni-icons type="right" size="16" color="#999" />
      </view>
      <view class="menu-row" @click="goChat">
        <view class="menu-left">
          <uni-icons type="headphones" size="20" color="#7B61FF" />
          <text>AI 客服</text>
        </view>
        <uni-icons type="right" size="16" color="#999" />
      </view>
      <view class="menu-row" @click="handleAbout">
        <view class="menu-left">
          <uni-icons type="info" size="20" color="#1890ff" />
          <text>关于</text>
        </view>
        <uni-icons type="right" size="16" color="#999" />
      </view>
    </view>

    <!-- 退出登录 -->
    <view v-if="userStore.isLogin" class="logout-btn" @click="handleLogout">退出登录</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'

const userStore = useUserStore()
const unpaidCount = ref(0)

const loadUnpaidCount = async () => {
  if (!userStore.isLogin) return
  try {
    const data = await request('/user/order/historyOrders', 'GET')
    unpaidCount.value = (data || []).filter(o => o.status === 1).length
  } catch (e) {}
}

const goLogin = () => uni.navigateTo({ url: '/pages/login/index' })
const goOrders = () => uni.navigateTo({ url: '/pages/order/list' })
const goOrdersByStatus = (status) => uni.navigateTo({ url: '/pages/order/list' })
const goAddress = () => uni.navigateTo({ url: '/pages/address/list' })
const goChat = () => uni.navigateTo({ url: '/pages/chat/index' })

const handleAbout = () => {
  uni.showModal({
    title: '关于苍穹外卖',
    content: '苍穹外卖 v1.0.0\n基于 uni-app 开发的外卖小程序',
    showCancel: false
  })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
      }
    }
  })
}

onMounted(() => {
  loadUnpaidCount()
})
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 60rpx;
}

.user-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(180deg, #FFC300 0%, #FFD54F 100%);
  padding: 50rpx 40rpx;
  padding-top: calc(60rpx + env(safe-area-inset-top));
  margin: -20rpx -20rpx 20rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-detail {
  margin-left: 24rpx;
}

.user-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
}

.user-id {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 6rpx;
}

.login-btn {
  background-color: #fff;
  color: #FFC300;
  padding: 14rpx 32rpx;
  border-radius: 50rpx;
  font-size: 26rpx;
  font-weight: 500;
}

.order-entry {
  padding: 0;
  margin-bottom: 20rpx;
}

.order-entry-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.order-entry-grid {
  display: flex;
  padding: 30rpx 0;
}

.grid-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.grid-icon {
  position: relative;
}

.grid-badge {
  position: absolute;
  top: -10rpx;
  right: -14rpx;
  background-color: #FF4B33;
  color: #fff;
  font-size: 18rpx;
  min-width: 28rpx;
  height: 28rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6rpx;
}

.grid-label {
  font-size: 24rpx;
  color: #666;
  margin-top: 12rpx;
}

.menu-list {
  padding: 0 30rpx;
  margin-bottom: 20rpx;
}

.menu-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.menu-left {
  display: flex;
  align-items: center;
  font-size: 28rpx;
  color: #333;

  text {
    margin-left: 16rpx;
  }
}

.logout-btn {
  background-color: #fff;
  color: #FF4B33;
  height: 88rpx;
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  margin-top: 20rpx;
}
</style>
