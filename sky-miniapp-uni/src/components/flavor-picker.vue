<template>
  <view class="flavor-mask" v-if="visible" @click="close">
    <view class="flavor-picker" @click.stop>
      <view class="picker-header">
        <text class="picker-title">{{ dish.name }}</text>
        <uni-icons type="closeempty" size="20" color="#999" @click="close" />
      </view>
      <scroll-view scroll-y class="flavor-content">
        <view class="flavor-group" v-for="group in flavorGroups" :key="group.name">
          <text class="group-title">{{ group.name }}</text>
          <view class="group-options">
            <view
              v-for="opt in group.options"
              :key="opt"
              class="opt-item"
              :class="{ active: isSelected(group.name, opt) }"
              @click="toggleFlavor(group.name, opt)"
            >
              {{ opt }}
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="picker-footer">
        <view class="price-row">
          <text class="price-label">价格</text>
          <text class="price-value">¥{{ dish.price }}</text>
        </view>
        <view class="confirm-btn" @click="handleConfirm">加入购物车</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  dish: Object
})

const emit = defineEmits(['update:visible', 'confirm'])

const flavorGroups = ref([])
const selectedFlavors = ref(new Map())

watch(() => props.dish, (val) => {
  if (val && val.flavors) {
    // 解析口味选项，格式：[{name: '辣度', options: ['不辣','微辣','中辣','特辣']}]
    try {
      const parsed = typeof val.flavors === 'string' ? JSON.parse(val.flavors) : val.flavors
      if (Array.isArray(parsed)) {
        flavorGroups.value = parsed
      }
    } catch (e) {
      flavorGroups.value = []
    }
  } else {
    flavorGroups.value = []
  }
  selectedFlavors.value = new Map()
}, { immediate: true })

const isSelected = (group, opt) => {
  return selectedFlavors.value.get(group) === opt
}

const toggleFlavor = (group, opt) => {
  if (selectedFlavors.value.get(group) === opt) {
    selectedFlavors.value.delete(group)
  } else {
    selectedFlavors.value.set(group, opt)
  }
}

const close = () => {
  emit('update:visible', false)
}

const handleConfirm = () => {
  const flavors = Array.from(selectedFlavors.value.values())
  emit('confirm', { ...props.dish, flavors })
  close()
}
</script>

<style lang="scss" scoped>
.flavor-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 998;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.flavor-picker {
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 75vh;
  display: flex;
  flex-direction: column;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1rpx solid #eee;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.flavor-content {
  flex: 1;
  padding: 20rpx 30rpx;
}

.flavor-group {
  margin-bottom: 30rpx;
}

.group-title {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 16rpx;
  display: block;
}

.group-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.opt-item {
  padding: 12rpx 28rpx;
  border-radius: 50rpx;
  background-color: #f5f5f5;
  font-size: 26rpx;
  color: #666;
  border: 1rpx solid transparent;
}

.opt-item.active {
  background-color: #FFF8E0;
  color: #333;
  border-color: #FFC300;
}

.picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-top: 1rpx solid #eee;
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));
}

.price-row {
  display: flex;
  align-items: baseline;
}

.price-label {
  font-size: 26rpx;
  color: #666;
  margin-right: 10rpx;
}

.price-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #FF4B33;
}

.confirm-btn {
  background-color: #FFC300;
  color: #fff;
  padding: 20rpx 50rpx;
  border-radius: 50rpx;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
