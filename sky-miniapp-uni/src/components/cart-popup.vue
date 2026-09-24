<template>
  <view class="cart-popup-mask" v-if="visible" @click="close">
    <view class="cart-popup" @click.stop>
      <view class="popup-header">
        <text class="popup-title">已选商品</text>
        <view class="clear-btn" @click="handleClear">
          <uni-icons type="trash" size="16" color="#999" />
          <text>清空</text>
        </view>
      </view>
      <scroll-view scroll-y class="cart-list">
        <view class="cart-item" v-for="item in cartItems" :key="item.id">
          <view class="item-left">
            <text class="item-name">{{ item.name }}</text>
            <text v-if="item.flavors && item.flavors.length" class="item-flavor">
              {{ item.flavors.join(' / ') }}
            </text>
          </view>
          <text class="item-price">¥{{ item.amount }}</text>
          <view class="item-actions">
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
      <view class="popup-footer">
        <view class="cart-summary">
          <text class="total-label">合计</text>
          <text class="total-price">¥{{ totalAmount.toFixed(2) }}</text>
        </view>
        <view class="checkout-btn" @click="handleCheckout">去结算({{ totalQuantity }})</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useCartStore } from '../../stores/cart'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'checkout'])

const cartStore = useCartStore()

const cartItems = computed(() => cartStore.items)
const totalQuantity = computed(() => cartStore.totalQuantity)
const totalAmount = computed(() => cartStore.totalAmount)

const close = () => {
  emit('update:visible', false)
}

const handleClear = async () => {
  uni.showModal({
    title: '提示',
    content: '确定清空购物车吗？',
    success: async (res) => {
      if (res.confirm) {
        await cartStore.clearCart()
        close()
      }
    }
  })
}

const handleIncrease = async (item) => {
  await cartStore.increase(item)
}

const handleDecrease = async (item) => {
  await cartStore.decrease(item)
}

const handleCheckout = () => {
  if (cartStore.isEmpty) {
    uni.showToast({ title: '购物车为空', icon: 'none' })
    return
  }
  close()
  emit('checkout')
}
</script>

<style lang="scss" scoped>
.cart-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.cart-popup {
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1rpx solid #eee;
}

.popup-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.clear-btn {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #999;
  text {
    margin-left: 6rpx;
  }
}

.cart-list {
  flex: 1;
  padding: 0 30rpx;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.item-left {
  flex: 1;
}

.item-name {
  font-size: 28rpx;
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
  margin-right: 20rpx;
}

.item-actions {
  display: flex;
  align-items: center;
}

.qty-btn {
  width: 44rpx;
  height: 44rpx;
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

.popup-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-top: 1rpx solid #eee;
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));
}

.cart-summary {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 26rpx;
  color: #666;
  margin-right: 10rpx;
}

.total-price {
  font-size: 36rpx;
  font-weight: 600;
  color: #FF4B33;
}

.checkout-btn {
  background-color: #FFC300;
  color: #fff;
  padding: 20rpx 50rpx;
  border-radius: 50rpx;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
