<template>
  <view class="confirm-page">
    <!-- 地址选择 -->
    <view class="address-section card" @click="goAddressList">
      <view v-if="selectedAddress" class="address-info">
        <view class="addr-top">
          <text class="addr-name">{{ selectedAddress.consignee }}</text>
          <text class="addr-phone">{{ selectedAddress.phone }}</text>
          <view v-if="selectedAddress.isDefault === 1" class="addr-default">默认</view>
        </view>
        <text class="addr-detail">{{ fullAddress }}</text>
      </view>
      <view v-else class="address-empty">
        <text>请选择收货地址</text>
      </view>
      <uni-icons type="right" size="16" color="#999" />
    </view>

    <!-- 店铺 -->
    <view class="shop-section card">
      <text class="shop-name">苍穹外卖</text>
    </view>

    <!-- 菜品清单 -->
    <view class="dishes-section card">
      <view class="dish-row" v-for="item in cartStore.items" :key="item.id">
        <image class="dish-img" :src="item.image || 'https://via.placeholder.com/90x90?text=Dish'" mode="aspectFill" />
        <view class="dish-info">
          <text class="dish-name">{{ item.name }}</text>
          <text v-if="item.flavors && item.flavors.length" class="dish-flavor">
            {{ item.flavors.join(' / ') }}
          </text>
        </view>
        <text class="dish-qty">x{{ item.number }}</text>
        <text class="dish-price">¥{{ item.amount }}</text>
      </view>
    </view>

    <!-- 金额明细 -->
    <view class="amount-section card">
      <view class="amount-row">
        <text>商品金额</text>
        <text>¥{{ cartStore.totalAmount.toFixed(2) }}</text>
      </view>
      <view class="amount-row">
        <text>打包费</text>
        <text>¥{{ packingFee.toFixed(2) }}</text>
      </view>
      <view class="amount-row">
        <text>配送费</text>
        <text>¥{{ deliveryFee.toFixed(2) }}</text>
      </view>
    </view>

    <!-- 备注 -->
    <view class="remark-section card">
      <text class="remark-label">备注</text>
      <input
        class="remark-input"
        v-model="remark"
        placeholder="请输入备注信息（选填）"
        placeholder-style="color:#ccc"
      />
    </view>

    <!-- 底部提交 -->
    <view class="confirm-footer">
      <view class="footer-total">
        <text>合计</text>
        <text class="total-price">¥{{ totalAmount.toFixed(2) }}</text>
      </view>
      <view class="submit-btn" @click="handleSubmit">提交订单</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCartStore } from '../../stores/cart'
import request from '../../utils/request'

const cartStore = useCartStore()
const addresses = ref([])
const selectedAddress = ref(null)
const remark = ref('')
const packingFee = ref(2)
const deliveryFee = ref(5)

const fullAddress = computed(() => {
  if (!selectedAddress.value) return ''
  const a = selectedAddress.value
  return `${a.provinceName || ''}${a.cityName || ''}${a.districtName || ''}${a.detail || ''}`
})

const totalAmount = computed(() => {
  return cartStore.totalAmount + packingFee.value + deliveryFee.value
})

const loadAddresses = async () => {
  try {
    const data = await request('/user/addressBook/list', 'GET')
    addresses.value = data || []
    // 选择默认地址
    const defaultAddr = addresses.value.find(a => a.isDefault === 1)
    selectedAddress.value = defaultAddr || addresses.value[0] || null
  } catch (e) {
    addresses.value = []
  }
}

const goAddressList = () => {
  uni.navigateTo({ url: '/pages/address/list?select=1' })
}

const handleSubmit = async () => {
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  if (cartStore.isEmpty) {
    uni.showToast({ title: '购物车为空', icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: '提交中...' })
    const orderData = {
      addressBookId: selectedAddress.value.id,
      remark: remark.value,
      packingFee: packingFee.value,
      deliveryFee: deliveryFee.value
    }
    const result = await request('/user/order/submit', 'POST', orderData)
    await cartStore.clearCart()
    uni.hideLoading()
    // 跳转到支付或订单详情
    uni.redirectTo({ url: `/pages/order/detail?id=${result.id}` })
  } catch (e) {
    uni.hideLoading()
  }
}

onMounted(() => {
  loadAddresses()
  cartStore.loadCart()
  uni.$on('addressSelected', (addr) => {
    selectedAddress.value = addr
  })
})

onUnmounted(() => {
  uni.$off('addressSelected')
})
</script>

<style lang="scss" scoped>
.confirm-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 160rpx;
}

.address-section {
  display: flex;
  align-items: center;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.address-info {
  flex: 1;
}

.addr-top {
  display: flex;
  align-items: center;
  margin-bottom: 10rpx;
}

.addr-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-right: 20rpx;
}

.addr-phone {
  font-size: 28rpx;
  color: #666;
}

.addr-default {
  font-size: 20rpx;
  color: #FFC300;
  border: 1rpx solid #FFC300;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  margin-left: 16rpx;
}

.addr-detail {
  font-size: 26rpx;
  color: #666;
}

.address-empty {
  flex: 1;
  font-size: 28rpx;
  color: #999;
}

.shop-section {
  padding: 24rpx 30rpx;
  margin-bottom: 20rpx;
}

.shop-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.dishes-section {
  padding: 10rpx 30rpx;
  margin-bottom: 20rpx;
}

.dish-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.dish-img {
  width: 90rpx;
  height: 90rpx;
  border-radius: 8rpx;
}

.dish-info {
  flex: 1;
  margin-left: 20rpx;
}

.dish-name {
  font-size: 28rpx;
  color: #333;
}

.dish-flavor {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.dish-qty {
  font-size: 26rpx;
  color: #666;
  margin-right: 20rpx;
}

.dish-price {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.amount-section {
  padding: 10rpx 30rpx;
  margin-bottom: 20rpx;
}

.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: #666;
}

.remark-section {
  display: flex;
  align-items: center;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.remark-label {
  font-size: 28rpx;
  color: #333;
  margin-right: 20rpx;
}

.remark-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.confirm-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  border-top: 1rpx solid #eee;
}

.footer-total {
  display: flex;
  align-items: baseline;

  text {
    font-size: 26rpx;
    color: #666;
    margin-right: 10rpx;
  }
}

.total-price {
  font-size: 36rpx;
  font-weight: 600;
  color: #FF4B33;
  margin-left: 10rpx;
}

.submit-btn {
  background-color: #FFC300;
  color: #fff;
  padding: 20rpx 60rpx;
  border-radius: 50rpx;
  font-size: 30rpx;
  font-weight: 500;
}
</style>
