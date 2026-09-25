<template>
  <view class="login-page">
    <view class="login-logo">
      <view class="logo-icon">🍱</view>
      <view class="logo-name">苍穹外卖</view>
      <view class="logo-sub">新鲜速达 · 美味到家</view>
    </view>

    <view class="login-card">
      <view class="login-title">微信账号登录</view>
      <view class="login-desc">登录后可享受更多服务</view>

      <button
        class="wx-login-btn"
        open-type="getPhoneNumber"
        @getphonenumber="onGetPhoneNumber"
      >
        <text class="wx-icon">💚</text>
        <text>微信一键登录</text>
      </button>

      <view class="login-tip">登录即代表同意《用户协议》和《隐私政策》</view>
    </view>
  </view>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

function onGetPhoneNumber(e) {
  // 实际开发中，需将 e.detail.code 传给后端换取手机号
  // 这里简化为直接走登录流程
  doLogin()
}

function doLogin() {
  userStore.login().then(() => {
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/index/index' })
      }
    }, 1000)
  }).catch((err) => {
    console.error('登录失败', err)
    // 演示模式：即使没有真微信环境也模拟登录成功
    uni.showModal({
      title: '演示模式',
      content: '当前环境不支持微信登录，是否模拟登录？',
      success(res) {
        if (res.confirm) {
          simulateLogin()
        }
      }
    })
  })
}

function simulateLogin() {
  const mockUser = {
    id: 1,
    openid: 'mock_openid_' + Date.now(),
    nickname: '苍穹用户',
    avatar: ''
  }
  userStore.setUserInfo(mockUser)
  // 模拟 token
  const mockToken = 'mock_token_' + Date.now()
  userStore.token = mockToken
  uni.setStorageSync('token', mockToken)
  uni.setStorageSync('userInfo', mockUser)
  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      uni.navigateBack()
    } else {
      uni.switchTab({ url: '/pages/index/index' })
    }
  }, 1000)
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFC300 0%, #FFF8E0 40%, #F5F4F1 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 40rpx 40rpx;
}

.login-logo {
  text-align: center;
  margin-bottom: 120rpx;

  .logo-icon {
    font-size: 160rpx;
    margin-bottom: 24rpx;
  }

  .logo-name {
    font-size: 48rpx;
    font-weight: 800;
    color: #2B2B2B;
    letter-spacing: 4rpx;
  }

  .logo-sub {
    font-size: 26rpx;
    color: rgba(43, 43, 43, 0.7);
    margin-top: 12rpx;
  }
}

.login-card {
  width: 100%;
  background: var(--sky-card);
  border-radius: var(--radius-xl);
  padding: 60rpx 40rpx;
  box-shadow: var(--shadow-float);
  text-align: center;

  .login-title {
    font-size: 34rpx;
    font-weight: 700;
    color: var(--sky-title);
  }

  .login-desc {
    font-size: 24rpx;
    color: var(--sky-muted);
    margin-top: 12rpx;
    margin-bottom: 60rpx;
  }

  .wx-login-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #07C160 0%, #05A050 100%);
    color: #FFFFFF;
    font-size: 30rpx;
    font-weight: 600;
    border-radius: 50rpx;
    padding: 24rpx 0;
    margin-bottom: 32rpx;
    box-shadow: 0 8rpx 20rpx rgba(7, 193, 96, 0.35);

    .wx-icon {
      font-size: 32rpx;
      margin-right: 12rpx;
    }
  }

  .login-tip {
    font-size: 22rpx;
    color: var(--sky-muted);
  }
}
</style>
