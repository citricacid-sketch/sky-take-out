<template>
  <view class="profile-page">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="user-bg" />
      <view class="user-info">
        <image class="user-avatar" :src="avatar" mode="aspectFill" />
        <view class="user-detail">
          <view class="user-name">{{ displayName }}</view>
          <view class="user-id">ID: {{ userInfo ? userInfo.id : '--' }}</view>
        </view>
        <view v-if="!isLoggedIn" class="login-btn" @click="goLogin">立即登录</view>
      </view>
    </view>

    <!-- 订单快捷入口 -->
    <view class="order-entry card">
      <view class="order-entry-header" @click="goOrders">
        <text class="oeh-title">我的订单</text>
        <text class="oeh-arrow">›</text>
      </view>
      <view class="order-entry-grid">
        <view class="oe-item" @click="goOrders(1)">
          <text class="oe-icon">⏰</text>
          <text class="oe-label">待付款</text>
        </view>
        <view class="oe-item" @click="goOrders(2)">
          <text class="oe-icon">📋</text>
          <text class="oe-label">待接单</text>
        </view>
        <view class="oe-item" @click="goOrders(4)">
          <text class="oe-icon">🛵</text>
          <text class="oe-label">派送中</text>
        </view>
        <view class="oe-item" @click="goOrders(5)">
          <text class="oe-icon">✅</text>
          <text class="oe-label">已完成</text>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-card card">
      <view class="menu-item" @click="goAddress">
        <text class="menu-icon">📍</text>
        <text class="menu-label">收货地址</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goChat">
        <text class="menu-icon">💬</text>
        <text class="menu-label">智能客服</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goRecommend">
        <text class="menu-icon">💛</text>
        <text class="menu-label">猜你喜欢</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view v-if="isLoggedIn" class="logout-btn" @click="onLogout">退出登录</view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

const displayName = computed(() => {
  if (!isLoggedIn.value) return '点击登录'
  return userInfo.value.nickname || '微信用户'
})

const avatar = computed(() => {
  if (!isLoggedIn.value) return '/static/images/default-avatar.png'
  return userInfo.value.avatar || '/static/images/default-avatar.png'
})

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goOrders(status) {
  if (!isLoggedIn.value) return goLogin()
  const url = status ? `/pages/order-list/order-list?status=${status}` : '/pages/order-list/order-list'
  uni.navigateTo({ url })
}

function goAddress() {
  if (!isLoggedIn.value) return goLogin()
  uni.navigateTo({ url: '/pages/address/address' })
}

function goChat() {
  uni.navigateTo({ url: '/pages/chat/chat' })
}

function goRecommend() {
  uni.switchTab({ url: '/pages/index/index' })
}

function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success(res) {
      if (res.confirm) {
        userStore.logout()
        uni.showToast({ title: '已退出', icon: 'success' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: var(--sky-page);
}

.user-card {
  position: relative;
  padding: 48rpx 32rpx;
  overflow: hidden;

  .user-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 200rpx;
    background: linear-gradient(135deg, #FFC300 0%, #FFA500 100%);
    z-index: 0;
  }

  .user-info {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;

    .user-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      border: 4rpx solid #FFFFFF;
      background: #F0F0F0;
    }

    .user-detail {
      flex: 1;
      margin-left: 24rpx;

      .user-name {
        font-size: 34rpx;
        font-weight: 700;
        color: #2B2B2B;
      }

      .user-id {
        font-size: 22rpx;
        color: rgba(43, 43, 43, 0.7);
        margin-top: 6rpx;
      }
    }

    .login-btn {
      background: rgba(255, 255, 255, 0.3);
      color: #2B2B2B;
      font-size: 26rpx;
      font-weight: 600;
      padding: 14rpx 32rpx;
      border-radius: 40rpx;
    }
  }
}

.order-entry {
  margin-top: -40rpx;
  position: relative;
  z-index: 2;

  .order-entry-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 24rpx;
    border-bottom: 1rpx solid var(--sky-border);

    .oeh-title {
      font-size: 28rpx;
      font-weight: 700;
      color: var(--sky-title);
    }

    .oeh-arrow {
      font-size: 32rpx;
      color: var(--sky-muted);
    }
  }

  .order-entry-grid {
    display: flex;
    padding-top: 24rpx;

    .oe-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;

      .oe-icon {
        font-size: 44rpx;
        margin-bottom: 8rpx;
      }

      .oe-label {
        font-size: 24rpx;
        color: var(--sky-body);
      }
    }
  }
}

.menu-card {
  .menu-item {
    display: flex;
    align-items: center;
    padding: 28rpx 0;
    border-bottom: 1rpx solid var(--sky-border);

    &:last-child {
      border-bottom: none;
    }

    .menu-icon {
      font-size: 32rpx;
      margin-right: 20rpx;
    }

    .menu-label {
      flex: 1;
      font-size: 28rpx;
      color: var(--sky-title);
    }

    .menu-arrow {
      font-size: 32rpx;
      color: var(--sky-muted);
    }
  }
}

.logout-btn {
  margin: var(--space-l) var(--space-s);
  text-align: center;
  padding: 24rpx;
  font-size: 28rpx;
  color: var(--sky-danger);
  background: var(--sky-card);
  border-radius: var(--radius-m);
}
</style>
