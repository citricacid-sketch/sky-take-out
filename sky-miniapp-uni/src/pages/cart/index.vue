<template>
  <view class="cart-page">
    <view v-if="cartStore.isEmpty" class="empty-state">
      <uni-icons type="cart" size="80" color="#ddd" />
      <text class="empty-text">购物车空空如也</text>
      <view class="go-menu-btn" @click="goMenu">去逛逛</view>
    </view>

    <view v-else class="cart-content">
      <scroll-view scroll-y class="cart-list">
        <view class="cart-item" v-for="item in cartStore.items" :key="item.id">
          <image class="item-image" :src="item.image || 'https://via.placeholder.com/90x90?text=Dish'" mode="aspectFill" />
          <view class="item-info">
            <text class="item-name">{{ item.name }}</text>
            <text v-if="item.flavors && item.flavors.length" class="item-flavor">
              {{ item.flavors.join(' / ') }}
            </text>
            <text class="item-price">¥{{ item.amount }}</text>
          </view>
          <view class="item-qty">
            <view class="qty-btn" @click="handleDecrease(item)">
              <uni-icons type="minusempty" size="14" color="#FFC300" />
            </view>
            <text class="qty-num">{{ item.number }}</text>
            <view class="qty-btn" @click="handleIncrease(item)">
              <uni-icons type="plusempty" size="14" color="#FFC300" />
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="cart-footer">
        <view class="footer-info">
          <text class="footer-label">合计</text>
          <text class="footer-total">¥{{ cartStore.totalAmount.toFixed(2) }}</text>
        </view>
        <view class="footer-btn" @click="handleCheckout">去结算({{ cartStore.totalQuantity }})</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { useCartStore } from '../../stores/cart'

const cartStore = useCartStore()

const handleIncrease = async (item) => {
  await cartStore.increase(item)
}

const handleDecrease = async (item) => {
  await cartStore.decrease(item)
}

const handleCheckout = () => {
  if (cartStore.isEmpty) return
  uni.navigateTo({ url: '/pages/order/confirm' })
}

const goMenu = () => uni.switchTab({ url: '/pages/menu/index' })
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.empty-state {
  flex: 1;
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
  margin-bottom: 40rpx;
}

.go-menu-btn {
  background-color: #FFC300;
  color: #fff;
  padding: 20rpx 60rpx;
  border-radius: 50rpx;
  font-size: 28rpx;
}

.cart-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.cart-list {
  flex: 1;
  padding: 20rpx;
}

.cart-item {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.item-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
}

.item-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.item-flavor {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.item-price {
  font-size: 28rpx;
  font-weight: 600;
  color: #FF4B33;
  margin-top: 12rpx;
}

.item-qty {
  display: flex;
  align-items: center;
}

.qty-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 1rpx solid #FFC300;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qty-num {
  font-size: 28rpx;
  color: #333;
  margin: 0 20rpx;
  min-width: 40rpx;
  text-align: center;
}

.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  border-top: 1rpx solid #eee;
}

.footer-info {
  display: flex;
  align-items: baseline;
}

.footer-label {
  font-size: 26rpx;
  color: #666;
  margin-right: 10rpx;
}

.footer-total {
  font-size: 36rpx;
  font-weight: 600;
  color: #FF4B33;
}

.footer-btn {
  background-color: #FFC300;
  color: #fff;
  padding: 20rpx 50rpx;
  border-radius: 50rpx;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
