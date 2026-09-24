<template>
  <view class="shop-status-badge" :class="status === 1 ? 'open' : 'closed'">
    <view class="dot" />
    <text>{{ status === 1 ? '营业中' : '打烊中' }}</text>
  </view>
</template>

<script setup>
defineProps({
  status: {
    type: Number,
    default: 1
  }
})
</script>

<style lang="scss" scoped>
.shop-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 16rpx;
  border-radius: 50rpx;
  font-size: 22rpx;

  .dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    margin-right: 8rpx;
  }

  &.open {
    background-color: #E8F5E9;
    color: #4CAF50;
    .dot { background-color: #4CAF50; }
  }

  &.closed {
    background-color: #FFF3E0;
    color: #FF9800;
    .dot { background-color: #FF9800; }
  }
}
</style>
