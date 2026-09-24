<template>
  <view class="dish-card" @click="handleClick">
    <image class="dish-image" :src="dish.image || 'https://via.placeholder.com/120x120?text=Dish'" mode="aspectFill" />
    <view class="dish-info">
      <text class="dish-name">{{ dish.name }}</text>
      <text class="dish-desc">{{ dish.description || '美味佳肴' }}</text>
      <view class="dish-meta">
        <text class="dish-sales">月售 {{ dish.sales || 0 }}</text>
      </view>
      <view class="dish-bottom">
        <text class="dish-price">¥{{ dish.price }}</text>
        <view class="add-btn" @click.stop="handleAdd">
          <uni-icons type="plusempty" size="16" color="#fff" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  dish: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click', 'add'])

const handleClick = () => {
  emit('click', props.dish)
}

const handleAdd = () => {
  emit('add', props.dish)
}
</script>

<style lang="scss" scoped>
.dish-card {
  display: flex;
  background-color: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.dish-image {
  width: 180rpx;
  height: 180rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.dish-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.dish-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-desc {
  font-size: 24rpx;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 6rpx;
}

.dish-meta {
  margin-top: 6rpx;
}

.dish-sales {
  font-size: 22rpx;
  color: #999;
}

.dish-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dish-price {
  font-size: 32rpx;
  font-weight: 600;
  color: #FF4B33;
}

.add-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background-color: #FFC300;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
