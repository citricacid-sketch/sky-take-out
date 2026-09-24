<template>
  <view class="login-page">
    <view class="login-bg">
      <view class="logo-area">
        <view class="logo-icon">
          <text class="logo-text">苍</text>
        </view>
        <text class="app-name">苍穹外卖</text>
        <text class="app-slogan">美味即刻送达</text>
      </view>
    </view>
    <view class="login-actions">
      <button class="wx-login-btn" @click="handleLogin">
        <uni-icons type="weixin" size="20" color="#fff" />
        <text>微信一键登录</text>
      </button>
      <view class="agreement-row">
        <text class="agreement-text">登录即代表同意《用户协议》和《隐私政策》</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const handleLogin = async () => {
  try {
    uni.showLoading({ title: '登录中...' })
    await userStore.login()
    uni.hideLoading()
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.login-bg {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #FFC300 0%, #FFD54F 100%);
}

.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo-icon {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
  margin-bottom: 30rpx;
}

.logo-text {
  font-size: 60rpx;
  font-weight: 700;
  color: #FFC300;
}

.app-name {
  font-size: 44rpx;
  font-weight: 600;
  color: #fff;
  margin-bottom: 10rpx;
}

.app-slogan {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.login-actions {
  padding: 60rpx 60rpx;
  padding-bottom: calc(60rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  border-radius: 30rpx 30rpx 0 0;
  margin-top: -30rpx;
}

.wx-login-btn {
  background-color: #07C160;
  color: #fff;
  border-radius: 50rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 500;
  border: none;

  text {
    margin-left: 10rpx;
  }

  &::after {
    border: none;
  }
}

.agreement-row {
  margin-top: 30rpx;
  text-align: center;
}

.agreement-text {
  font-size: 22rpx;
  color: #999;
}
</style>
