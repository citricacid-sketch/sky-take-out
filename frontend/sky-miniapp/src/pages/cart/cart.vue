<template>
  <view class="cart-page">
    <view v-if="cartStore.items.length > 0" class="cart-content">
      <!-- 顶部提示 -->
      <view class="cart-tip">
        <text class="tip-icon">💡</text>
        <text>已选 {{ cartStore.totalCount }} 件商品</text>
      </view>

      <!-- 商品列表 -->
      <view class="cart-list">
        <view
          v-for="item in cartStore.items"
          :key="item.id"
          class="cart-item"
        >
          <image class="item-image" :src="item.image || defaultImage" mode="aspectFill" />
          <view class="item-info">
            <view class="item-name text-ellipsis">{{ item.name }}</view>
            <view v-if="item.dishFlavor" class="item-flavor">{{ item.dishFlavor }}</view>
            <view class="item-bottom">
              <view class="price">
                <text class="symbol">¥</text>
                <text class="value-small">{{ formatPrice(item.amount) }}</text>
              </view>
              <view class="item-stepper">
                <view class="step-btn step-btn-minus" @click="onSub(item)">−</view>
                <text class="step-count">{{ item.number }}</text>
                <view class="step-btn step-btn-plus" @click="onAdd(item)">+</view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 空购物车 -->
    <view v-else class="empty-state">
      <view class="empty-icon">🛒</view>
      <view class="empty-text">购物车空空如也</view>
      <view class="empty-action">
        <view class="btn-primary" @click="goHome">去逛逛</view>
      </view>
    </view>

    <!-- 底部结算条 -->
    <view v-if="cartStore.items.length > 0" class="cart-footer">
      <view class="footer-info">
        <view class="footer-label">合计</view>
        <view class="price">
          <text class="symbol">¥</text>
          <text class="value">{{ cartStore.totalAmount }}</text>
        </view>
      </view>
      <view class="footer-actions">
        <view class="btn-clear" @click="onClear">清空</view>
        <view class="btn-checkout" @click="goCheckout">去结算</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils'

const cartStore = useCartStore()
const defaultImage = '/static/images/default-dish.png'

function onAdd(item) {
  const dishId = item.dishId || null
  const setmealId = item.setmealId || null
  cartStore.add(dishId, setmealId, item.dishFlavor)
}

function onSub(item) {
  const dishId = item.dishId || null
  const setmealId = item.setmealId || null
  cartStore.sub(dishId, setmealId, item.dishFlavor)
}

function onClear() {
  uni.showModal({
    title: '提示',
    content: '确定清空购物车吗？',
    success(res) {
      if (res.confirm) {
        cartStore.clear()
      }
    }
  })
}

function goCheckout() {
  uni.navigateTo({ url: '/pages/checkout/checkout' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  padding-bottom: 140rpx;
  background: var(--sky-page);
}

.cart-tip {
  display: flex;
  align-items: center;
  background: var(--sky-primary-soft);
  color: var(--sky-primary-dark);
  padding: 16rpx 24rpx;
  font-size: 24rpx;

  .tip-icon {
    margin-right: 8rpx;
  }
}

.cart-list {
  padding: var(--space-s);
}

.cart-item {
  display: flex;
  background: var(--sky-card);
  border-radius: var(--radius-m);
  padding: var(--space-s);
  margin-bottom: var(--space-s);
  box-shadow: var(--shadow-card);

  .item-image {
    width: 160rpx;
    height: 160rpx;
    border-radius: var(--radius-s);
    flex-shrink: 0;
  }

  .item-info {
    flex: 1;
    margin-left: var(--space-s);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    min-width: 0;

    .item-name {
      font-size: 28rpx;
      font-weight: 600;
      color: var(--sky-title);
    }

    .item-flavor {
      font-size: 22rpx;
      color: var(--sky-muted);
      margin-top: 6rpx;
    }

    .item-bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .item-stepper {
      display: flex;
      align-items: center;
    }
  }
}

.empty-state {
  padding: 200rpx 40rpx;

  .empty-action {
    width: 300rpx;
    margin: 40rpx auto 0;
  }
}

.cart-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--sky-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);

  .footer-info {
    display: flex;
    align-items: baseline;

    .footer-label {
      font-size: 26rpx;
      color: var(--sky-body);
      margin-right: 8rpx;
    }
  }

  .footer-actions {
    display: flex;
    align-items: center;

    .btn-clear {
      font-size: 26rpx;
      color: var(--sky-muted);
      padding: 16rpx 32rpx;
      border: 2rpx solid var(--sky-border);
      border-radius: 40rpx;
      margin-right: 16rpx;
    }

    .btn-checkout {
      background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
      color: #2B2B2B;
      font-weight: 700;
      padding: 18rpx 48rpx;
      border-radius: 40rpx;
      font-size: 28rpx;
      box-shadow: 0 4rpx 12rpx rgba(255, 195, 0, 0.4);
    }
  }
}
</style>
