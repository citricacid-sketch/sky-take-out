<template>
  <view class="address-page">
    <view v-if="addresses.length === 0" class="empty-state">
      <uni-icons type="location" size="80" color="#ddd" />
      <text class="empty-text">暂无收货地址</text>
    </view>

    <view v-else class="address-list">
      <view
        class="address-item card"
        v-for="addr in addresses"
        :key="addr.id"
        @click="handleSelect(addr)"
      >
        <view class="addr-main">
          <view class="addr-top">
            <text class="addr-name">{{ addr.consignee }}</text>
            <text class="addr-phone">{{ addr.phone }}</text>
            <view v-if="addr.isDefault === 1" class="addr-tag">默认</view>
          </view>
          <text class="addr-detail">
            {{ addr.provinceName }}{{ addr.cityName }}{{ addr.districtName }}{{ addr.detail }}
          </text>
        </view>
        <view class="addr-edit" @click.stop="goEdit(addr.id)">
          <uni-icons type="compose" size="20" color="#999" />
        </view>
      </view>
    </view>

    <view class="add-btn" @click="goEdit()">
      <uni-icons type="plusempty" size="18" color="#fff" />
      <text>新增收货地址</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onLoad } from 'vue'
import request from '../../utils/request'

const addresses = ref([])
const isSelectMode = ref(false)

const loadAddresses = async () => {
  try {
    const data = await request('/user/addressBook/list', 'GET')
    addresses.value = data || []
  } catch (e) {
    addresses.value = []
  }
}

const handleSelect = (addr) => {
  if (isSelectMode.value) {
    // 选择模式：返回上一页并传递地址
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    if (prevPage) {
      uni.$emit('addressSelected', addr)
    }
    uni.navigateBack()
  } else {
    goEdit(addr.id)
  }
}

const goEdit = (id) => {
  const url = id ? `/pages/address/edit?id=${id}` : '/pages/address/edit'
  uni.navigateTo({ url })
}

onLoad((options) => {
  if (options && options.select === '1') {
    isSelectMode.value = true
  }
})

onMounted(() => {
  loadAddresses()
})
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.empty-state {
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
}

.address-list {
  padding-bottom: 20rpx;
}

.address-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.addr-main {
  flex: 1;
}

.addr-top {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
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

.addr-tag {
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
  line-height: 1.5;
}

.addr-edit {
  padding: 20rpx;
}

.add-btn {
  position: fixed;
  bottom: 40rpx;
  left: 40rpx;
  right: 40rpx;
  bottom: calc(40rpx + env(safe-area-inset-bottom));
  background-color: #FFC300;
  color: #fff;
  height: 88rpx;
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 500;

  text {
    margin-left: 10rpx;
  }
}
</style>
