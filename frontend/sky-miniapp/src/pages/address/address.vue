<template>
  <view class="address-page">
    <view v-if="addresses.length > 0" class="address-list">
      <view
        v-for="addr in addresses"
        :key="addr.id"
        class="address-card card"
        @click="onSelect(addr)"
      >
        <view class="addr-top">
          <view class="addr-user">
            <text class="addr-consignee">{{ addr.consignee }}</text>
            <text class="addr-phone">{{ addr.phone }}</text>
            <view v-if="addr.isDefault === 1" class="addr-default-tag">默认</view>
          </view>
          <view class="addr-edit" @click.stop="goEdit(addr)">编辑</view>
        </view>
        <view class="addr-detail">
          {{ addr.provinceName }}{{ addr.cityName }}{{ addr.districtName }}{{ addr.detail }}
        </view>
        <view v-if="addr.label" class="addr-label">
          <view class="tag tag-primary">{{ addr.label }}</view>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <view class="empty-icon">📍</view>
      <view class="empty-text">暂无收货地址</view>
    </view>

    <view class="bottom-bar">
      <view class="btn-primary" @click="goAdd">+ 新增地址</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAddressList } from '@/api/address'

const addresses = ref([])
const selectable = ref(false)

async function fetchList() {
  try {
    addresses.value = await getAddressList()
  } catch (e) {
    addresses.value = []
  }
}

function goAdd() {
  uni.navigateTo({ url: '/pages/address-edit/address-edit' })
}

function goEdit(addr) {
  uni.navigateTo({ url: `/pages/address-edit/address-edit?id=${addr.id}` })
}

function onSelect(addr) {
  if (selectable.value) {
    uni.$emit('address-selected', addr)
    uni.navigateBack()
  } else {
    goEdit(addr)
  }
}

onMounted((options) => {
  if (options && options.select === '1') selectable.value = true
  fetchList()
})
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  padding: var(--space-s);
  padding-bottom: 160rpx;
}

.address-list {
  .address-card {
    margin-bottom: var(--space-s);

    .addr-top {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .addr-user {
        display: flex;
        align-items: center;
        flex: 1;

        .addr-consignee {
          font-size: 28rpx;
          font-weight: 600;
          color: var(--sky-title);
        }

        .addr-phone {
          font-size: 24rpx;
          color: var(--sky-muted);
          margin-left: 16rpx;
        }

        .addr-default-tag {
          font-size: 20rpx;
          color: var(--sky-primary-dark);
          background: var(--sky-primary-soft);
          padding: 2rpx 12rpx;
          border-radius: var(--radius-s);
          margin-left: 12rpx;
        }
      }

      .addr-edit {
        font-size: 24rpx;
        color: var(--sky-info);
        padding: 8rpx 16rpx;
      }
    }

    .addr-detail {
      font-size: 24rpx;
      color: var(--sky-body);
      margin-top: 12rpx;
      line-height: 1.5;
    }

    .addr-label {
      margin-top: 12rpx;
    }
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: var(--sky-card);
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);
}
</style>
