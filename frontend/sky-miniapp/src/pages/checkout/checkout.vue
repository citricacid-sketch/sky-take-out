<template>
  <view class="checkout-page">
    <!-- 收货地址 -->
    <view class="address-card card" @click="goAddressList">
      <view v-if="selectedAddress" class="address-info">
        <view class="address-top">
          <text class="address-detail text-ellipsis">{{ fullAddress }}</text>
          <text class="address-arrow">›</text>
        </view>
        <view class="address-bottom">
          <text class="address-consignee">{{ selectedAddress.consignee }}</text>
          <text class="address-phone">{{ selectedAddress.phone }}</text>
        </view>
      </view>
      <view v-else class="address-empty">
        <text>📍 请选择收货地址</text>
        <text class="address-arrow">›</text>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="goods-card card">
      <view class="goods-header">已选商品</view>
      <view
        v-for="item in cartStore.items"
        :key="item.id"
        class="goods-item"
      >
        <image class="goods-img" :src="item.image || defaultImage" mode="aspectFill" />
        <view class="goods-mid">
          <view class="goods-name text-ellipsis">{{ item.name }}</view>
          <view v-if="item.dishFlavor" class="goods-flavor">{{ item.dishFlavor }}</view>
        </view>
        <view class="goods-right">
          <text class="goods-count">x{{ item.number }}</text>
          <view class="price">
            <text class="symbol">¥</text>
            <text class="value-small">{{ formatPrice(item.amount) }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 配送设置 -->
    <view class="delivery-card card">
      <view class="form-row">
        <text class="form-label">预计送达</text>
        <view class="form-value">{{ deliveryTimeText }}</view>
      </view>
      <view class="form-row">
        <text class="form-label">餐具数量</text>
        <view class="form-value" @click="showTablewarePicker = true">
          {{ tablewareText }}
          <text class="form-arrow">›</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">备注</text>
        <input
          class="form-input"
          v-model="remark"
          placeholder="选填，请输入备注"
          placeholder-style="color: #B8BFC9"
        />
      </view>
    </view>

    <!-- 金额明细 -->
    <view class="price-card card">
      <view class="price-row">
        <text>商品金额</text>
        <text>¥{{ cartStore.totalAmount }}</text>
      </view>
      <view class="price-row">
        <text>配送费</text>
        <text>¥{{ deliveryFee }}</text>
      </view>
      <view class="price-row total">
        <text>合计</text>
        <view class="price">
          <text class="symbol">¥</text>
          <text class="value">{{ totalAmount }}</text>
        </view>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="pay-card card">
      <view class="form-row">
        <text class="form-label">支付方式</text>
        <view class="form-value">
          <text class="pay-icon">💚</text>
          <text>微信支付</text>
        </view>
      </view>
    </view>

    <!-- 底部提交栏 -->
    <view class="submit-bar">
      <view class="submit-info">
        <text class="submit-label">合计</text>
        <view class="price">
          <text class="symbol">¥</text>
          <text class="value">{{ totalAmount }}</text>
        </view>
      </view>
      <view class="btn-submit" @click="onSubmit">提交订单</view>
    </view>

    <!-- 餐具选择弹窗 -->
    <view v-if="showTablewarePicker" class="picker-mask" @click="showTablewarePicker = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-title">餐具数量</view>
        <view
          v-for="opt in tablewareOptions"
          :key="opt.value"
          class="picker-option"
          :class="{ active: tablewareStatus === opt.value }"
          @click="tablewareStatus = opt.value; showTablewarePicker = false"
        >
          {{ opt.label }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useCartStore } from '@/stores/cart'
import { submitOrder } from '@/api/order'
import { getDefaultAddress } from '@/api/address'
import { formatPrice } from '@/utils'
import { requireLogin } from '@/utils'

const cartStore = useCartStore()
const defaultImage = '/static/images/default-dish.png'
const deliveryFee = ref(5)

const selectedAddress = ref(null)
const remark = ref('')
const deliveryStatus = ref(1) // 1立即送出
const tablewareStatus = ref(1) // 1按餐量提供
const showTablewarePicker = ref(false)

const tablewareOptions = [
  { value: 1, label: '按餐量提供' },
  { value: 0, label: '选择具体数量' }
]

const tablewareText = computed(() => {
  const opt = tablewareOptions.find(o => o.value === tablewareStatus.value)
  return opt ? opt.label : ''
})

const fullAddress = computed(() => {
  if (!selectedAddress.value) return ''
  const a = selectedAddress.value
  return `${a.provinceName || ''}${a.cityName || ''}${a.districtName || ''}${a.detail || ''}`
})

const deliveryTimeText = computed(() => {
  if (deliveryStatus.value === 1) return '立即送出'
  return '预约时间'
})

const totalAmount = computed(() => {
  return (Number(cartStore.totalAmount) + deliveryFee.value).toFixed(2)
})

async function fetchDefaultAddress() {
  try {
    selectedAddress.value = await getDefaultAddress()
  } catch (e) {
    selectedAddress.value = null
  }
}

function goAddressList() {
  uni.navigateTo({ url: '/pages/address/address?select=1' })
}

async function onSubmit() {
  if (!requireLogin()) return
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  if (cartStore.items.length === 0) {
    uni.showToast({ title: '购物车是空的', icon: 'none' })
    return
  }

  try {
    const data = await submitOrder({
      addressBookId: selectedAddress.value.id,
      payMethod: 1,
      remark: remark.value,
      deliveryStatus: deliveryStatus.value,
      tablewareStatus: tablewareStatus.value,
      packAmount: 0,
      amount: Number(totalAmount.value),
      estimatedDeliveryTime: new Date().toISOString()
    })
    // 清空购物车
    await cartStore.clear()
    // 跳转到订单详情
    uni.redirectTo({ url: `/pages/order-detail/order-detail?id=${data.id}` })
  } catch (e) {
    // 错误已在 request 中处理
  }
}

onMounted(() => {
  if (!requireLogin()) return
  fetchDefaultAddress()
  if (cartStore.items.length === 0) {
    cartStore.fetchCart()
  }
})
</script>

<style lang="scss" scoped>
.checkout-page {
  min-height: 100vh;
  padding: var(--space-s);
  padding-bottom: 160rpx;
}

.address-card {
  background: linear-gradient(135deg, #FFFDF7 0%, #FFF8E0 100%);
  border-left: 8rpx solid var(--sky-primary);

  .address-info {
    .address-top {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .address-detail {
        font-size: 30rpx;
        font-weight: 700;
        color: var(--sky-title);
        flex: 1;
      }

      .address-arrow {
        font-size: 36rpx;
        color: var(--sky-muted);
        margin-left: 16rpx;
      }
    }

    .address-bottom {
      margin-top: 12rpx;
      font-size: 24rpx;
      color: var(--sky-body);

      .address-consignee {
        margin-right: 20rpx;
      }
    }
  }

  .address-empty {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 28rpx;
    color: var(--sky-muted);

    .address-arrow {
      font-size: 36rpx;
      color: var(--sky-muted);
    }
  }
}

.goods-card {
  .goods-header {
    font-size: 26rpx;
    color: var(--sky-muted);
    margin-bottom: 16rpx;
  }

  .goods-item {
    display: flex;
    align-items: center;
    padding: 16rpx 0;

    .goods-img {
      width: 100rpx;
      height: 100rpx;
      border-radius: var(--radius-s);
    }

    .goods-mid {
      flex: 1;
      margin-left: 16rpx;
      min-width: 0;

      .goods-name {
        font-size: 26rpx;
        font-weight: 600;
        color: var(--sky-title);
      }

      .goods-flavor {
        font-size: 20rpx;
        color: var(--sky-muted);
        margin-top: 4rpx;
      }
    }

    .goods-right {
      text-align: right;

      .goods-count {
        font-size: 22rpx;
        color: var(--sky-muted);
      }

      .price {
        margin-top: 4rpx;
      }
    }
  }
}

.delivery-card,
.price-card,
.pay-card {
  .form-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20rpx 0;
    border-bottom: 1rpx solid var(--sky-border);

    &:last-child {
      border-bottom: none;
    }

    .form-label {
      font-size: 26rpx;
      color: var(--sky-body);
      width: 160rpx;
    }

    .form-value {
      font-size: 26rpx;
      color: var(--sky-title);
      display: flex;
      align-items: center;

      .form-arrow {
        font-size: 32rpx;
        color: var(--sky-muted);
        margin-left: 8rpx;
      }

      .pay-icon {
        margin-right: 8rpx;
      }
    }

    .form-input {
      flex: 1;
      text-align: right;
      font-size: 26rpx;
      color: var(--sky-title);
    }
  }
}

.price-card {
  .price-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 0;
    font-size: 26rpx;
    color: var(--sky-body);

    &.total {
      padding-top: 20rpx;
      margin-top: 8rpx;
      border-top: 1rpx solid var(--sky-border);
      font-weight: 600;
      color: var(--sky-title);
    }
  }
}

.submit-bar {
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

  .submit-info {
    display: flex;
    align-items: baseline;

    .submit-label {
      font-size: 24rpx;
      color: var(--sky-body);
      margin-right: 8rpx;
    }
  }

  .btn-submit {
    background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
    color: #2B2B2B;
    font-weight: 700;
    padding: 22rpx 64rpx;
    border-radius: 40rpx;
    font-size: 30rpx;
    box-shadow: 0 6rpx 16rpx rgba(255, 195, 0, 0.4);
  }
}

.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;

  .picker-panel {
    width: 100%;
    background: var(--sky-card);
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    padding: var(--space-m);

    .picker-title {
      font-size: 30rpx;
      font-weight: 700;
      color: var(--sky-title);
      text-align: center;
      padding-bottom: var(--space-m);
    }

    .picker-option {
      padding: 28rpx;
      text-align: center;
      font-size: 28rpx;
      color: var(--sky-body);
      border-bottom: 1rpx solid var(--sky-border);

      &.active {
        color: var(--sky-primary-dark);
        font-weight: 600;
        background: var(--sky-primary-soft);
        border-radius: var(--radius-s);
      }
    }
  }
}
</style>
