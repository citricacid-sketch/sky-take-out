<template>
  <view class="home-page">
    <!-- 顶部区域 -->
    <view class="header-section">
      <view class="header-info">
        <text class="greeting">您好，欢迎光临</text>
        <shop-status-badge :status="shopStore.status" />
      </view>
      <view class="header-bg" />
    </view>

    <!-- 快捷入口 -->
    <view class="quick-entry card">
      <view class="entry-item" @click="goMenu">
        <view class="entry-icon menu-icon">
          <uni-icons type="shop" size="24" color="#FFC300" />
        </view>
        <text class="entry-text">点餐</text>
      </view>
      <view class="entry-item" @click="goOrders">
        <view class="entry-icon order-icon">
          <uni-icons type="list" size="24" color="#1890ff" />
        </view>
        <text class="entry-text">订单</text>
      </view>
      <view class="entry-item" @click="goAddress">
        <view class="entry-icon addr-icon">
          <uni-icons type="location" size="24" color="#FF6B6B" />
        </view>
        <text class="entry-text">地址</text>
      </view>
      <view class="entry-item" @click="goChat">
        <view class="entry-icon chat-icon">
          <uni-icons type="headphones" size="24" color="#7B61FF" />
        </view>
        <text class="entry-text">客服</text>
      </view>
    </view>

    <!-- 推荐菜品 -->
    <view class="recommend-section">
      <view class="section-header">
        <text class="section-title">为你推荐</text>
        <text class="section-more" @click="goMenu">查看更多 ></text>
      </view>
      <scroll-view scroll-x class="recommend-scroll">
        <view class="recommend-list">
          <view
            class="recommend-card"
            v-for="dish in recommendList"
            :key="dish.id"
            @click="goMenu"
          >
            <image class="rec-image" :src="dish.image || 'https://via.placeholder.com/120x120?text=Dish'" mode="aspectFill" />
            <text class="rec-name">{{ dish.name }}</text>
            <text class="rec-price">¥{{ dish.price }}</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ShopStatusBadge from '../../components/shop-status-badge.vue'
import { useShopStore } from '../../stores/shop'
import request from '../../utils/request'

const shopStore = useShopStore()
const recommendList = ref([])

const loadRecommend = async () => {
  try {
    const data = await request('/user/recommend', 'GET')
    recommendList.value = data || []
  } catch (e) {
    recommendList.value = []
  }
}

const goMenu = () => uni.switchTab({ url: '/pages/menu/index' })
const goOrders = () => uni.navigateTo({ url: '/pages/order/list' })
const goAddress = () => uni.navigateTo({ url: '/pages/address/list' })
const goChat = () => uni.navigateTo({ url: '/pages/chat/index' })

onMounted(async () => {
  await shopStore.fetchStatus()
  loadRecommend()
})
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}

.header-section {
  position: relative;
  padding: 40rpx 30rpx;
  background: linear-gradient(180deg, #FFC300 0%, #FFD54F 100%);
  padding-top: calc(80rpx + env(safe-area-inset-top));
}

.header-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.greeting {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.quick-entry {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: -40rpx 30rpx 30rpx;
  padding: 30rpx 0;
  position: relative;
  z-index: 2;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.entry-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.menu-icon { background-color: #FFF8E0; }
.order-icon { background-color: #E6F4FF; }
.addr-icon { background-color: #FFF1F0; }
.chat-icon { background-color: #F0EBFF; }

.entry-text {
  font-size: 26rpx;
  color: #333;
}

.recommend-section {
  padding: 0 30rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.section-more {
  font-size: 24rpx;
  color: #999;
}

.recommend-scroll {
  white-space: nowrap;
}

.recommend-list {
  display: inline-flex;
  gap: 20rpx;
}

.recommend-card {
  display: inline-flex;
  flex-direction: column;
  width: 260rpx;
  background-color: #fff;
  border-radius: 12rpx;
  padding: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.rec-image {
  width: 228rpx;
  height: 228rpx;
  border-radius: 12rpx;
}

.rec-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-top: 12rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-price {
  font-size: 28rpx;
  color: #FF4B33;
  font-weight: 600;
  margin-top: 8rpx;
}
</style>
