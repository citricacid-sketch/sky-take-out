<template>
  <view class="flavor-popup" v-if="visible">
    <view class="flavor-mask" @click="close"></view>
    <view class="flavor-panel" :class="{ visible }">
      <view class="flavor-header">
        <image class="flavor-image" :src="dish.image || defaultImage" mode="aspectFill" />
        <view class="flavor-title">
          <view class="flavor-name">{{ dish.name }}</view>
          <view class="flavor-price">
            <text class="symbol">¥</text>
            <text class="value">{{ formatPrice(dish.price) }}</text>
          </view>
        </view>
        <view class="flavor-close" @click="close">
          <text class="flavor-close-text">×</text>
        </view>
      </view>

      <scroll-view scroll-y class="flavor-body">
        <view v-for="(flavor, idx) in dish.flavors" :key="idx" class="flavor-group">
          <view class="flavor-group-name">
            <text>{{ flavor.name }}</text>
            <text class="flavor-required">*</text>
          </view>
          <view class="flavor-options">
            <view
              v-for="(opt, oIdx) in parseOptions(flavor.value)"
              :key="oIdx"
              class="flavor-option"
              :class="{ active: isSelected(flavor.name, opt) }"
              @click="toggleOption(flavor.name, opt)"
            >
              <text>{{ opt }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="flavor-footer">
        <view class="btn-confirm" @click="confirm">加入购物车</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { formatPrice } from '@/utils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  dish: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:visible', 'confirm'])

const defaultImage = '/static/images/default-dish.png'
const selected = ref({})

function parseOptions(value) {
  if (!value) return []
  try {
    if (value.startsWith('[')) return JSON.parse(value)
    return value.split(',').map(s => s.trim()).filter(Boolean)
  } catch (e) {
    return value.split(',').map(s => s.trim()).filter(Boolean)
  }
}

function isSelected(name, opt) {
  return selected.value[name] === opt
}

function toggleOption(name, opt) {
  selected.value = { ...selected.value, [name]: opt }
}

function close() {
  emit('update:visible', false)
  selected.value = {}
}

function confirm() {
  const flavorKeys = (props.dish.flavors || []).map(f => f.name)
  const missing = flavorKeys.filter(k => !selected.value[k])
  if (missing.length > 0) {
    uni.showToast({ title: `请选择${missing[0]}`, icon: 'none' })
    return
  }
  const flavorStr = flavorKeys.map(k => `${k}:${selected.value[k]}`).join(',')
  emit('confirm', flavorStr)
  uni.showToast({ title: '已加入购物车', icon: 'success' })
  close()
}

watch(() => props.visible, (val) => {
  if (val) selected.value = {}
})
</script>

<style lang="scss" scoped>
.flavor-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;

  .flavor-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
  }

  .flavor-panel {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--color-bg-card);
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    max-height: 75vh;
    display: flex;
    flex-direction: column;
    transform: translateY(100%);
    transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.12);

    &.visible {
      transform: translateY(0);
    }
  }

  .flavor-header {
    display: flex;
    align-items: flex-start;
    padding: var(--space-6);
    padding-bottom: var(--space-4);
    position: relative;

    .flavor-image {
      width: 160rpx;
      height: 160rpx;
      border-radius: var(--radius-lg);
      margin-top: -48rpx;
      box-shadow: var(--shadow-lg);
      border: 4rpx solid var(--color-bg-card);
      object-fit: cover;
    }

    .flavor-title {
      margin-left: var(--space-5);
      flex: 1;
      padding-top: var(--space-2);

      .flavor-name {
        font-size: var(--text-lg);
        font-weight: var(--font-bold);
        color: var(--color-text-primary);
        line-height: 1.3;
      }

      .flavor-price {
        margin-top: var(--space-3);
        color: var(--color-accent);
        display: flex;
        align-items: baseline;

        .symbol {
          font-size: var(--text-sm);
          font-weight: var(--font-semibold);
        }

        .value {
          font-size: var(--text-2xl);
          font-weight: var(--font-bold);
        }
      }
    }

    .flavor-close {
      width: 56rpx;
      height: 56rpx;
      border-radius: 50%;
      background: var(--color-bg-muted);
      display: flex;
      align-items: center;
      justify-content: center;

      .flavor-close-text {
        font-size: 36rpx;
        color: var(--color-text-tertiary);
        line-height: 1;
        font-weight: 300;
      }
    }
  }

  .flavor-body {
    flex: 1;
    padding: 0 var(--space-6);
    overflow-y: auto;
  }

  .flavor-group {
    margin-top: var(--space-6);

    &:first-child {
      margin-top: var(--space-4);
    }

    .flavor-group-name {
      font-size: var(--text-base);
      color: var(--color-text-secondary);
      font-weight: var(--font-semibold);
      margin-bottom: var(--space-4);

      .flavor-required {
        color: var(--color-accent);
        margin-left: var(--space-1);
      }
    }

    .flavor-options {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-3);
    }

    .flavor-option {
      padding: var(--space-3) var(--space-5);
      border-radius: var(--radius-full);
      background: var(--color-bg-muted);
      font-size: var(--text-sm);
      color: var(--color-text-secondary);
      border: 1rpx solid transparent;
      transition: all 0.2s;

      &.active {
        background: var(--color-primary-soft);
        color: var(--color-primary-dark);
        border-color: var(--color-primary);
        font-weight: var(--font-semibold);
      }
    }
  }

  .flavor-footer {
    padding: var(--space-5) var(--space-6);
    padding-bottom: calc(var(--space-5) + env(safe-area-inset-bottom));
    background: var(--color-bg-card);
    border-top: 1rpx solid var(--color-divider);

    .btn-confirm {
      background: var(--color-primary);
      color: #FFFFFF;
      border-radius: var(--radius-full);
      text-align: center;
      padding: var(--space-5) 0;
      font-weight: var(--font-semibold);
      font-size: var(--text-base);
      box-shadow: 0 4rpx 12rpx rgba(232, 93, 58, 0.25);
      letter-spacing: 1rpx;
    }
  }
}
</style>
