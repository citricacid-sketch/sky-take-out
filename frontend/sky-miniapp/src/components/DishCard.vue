<template>
  <view class="dish-card" @click="onClick">
    <view class="dish-image-wrapper">
      <image class="dish-image" :src="item.image || defaultImage" mode="aspectFill" />
      <view v-if="item.status === 0" class="dish-mask">
        <text class="dish-mask-text">已停售</text>
      </view>
      <!-- 推荐角标 -->
      <view v-if="item.recommend" class="dish-badge">推荐</view>
    </view>
    <view class="dish-info">
      <view class="dish-name text-ellipsis">{{ item.name }}</view>
      <view class="dish-desc text-ellipsis-2">{{ item.description || '店家推荐，值得一试' }}</view>
      <view class="dish-meta">
        <text class="dish-sales">月售{{ item.sales || 0 }}</text>
        <text v-if="item.praise" class="dish-praise">好评{{ item.praise }}%</text>
      </view>
      <view class="dish-bottom">
        <view class="price">
          <text class="symbol">¥</text>
          <text class="value">{{ formatPrice(item.price) }}</text>
        </view>
        <!-- 有口味选择 -->
        <view v-if="hasFlavors" class="dish-action">
          <view class="btn-spec" @click.stop="onChooseFlavor">
            <text>选规格</text>
            <text v-if="flavorCount > 0" class="spec-count">{{ flavorCount }}</text>
          </view>
        </view>
        <!-- 无口味，直接加减 -->
        <view v-else class="dish-action flex">
          <view
            v-if="count > 0"
            class="step-btn step-btn-minus"
            @click.stop="onSubtract"
          >
            <text>−</text>
          </view>
          <text v-if="count > 0" class="step-count">{{ count }}</text>
          <view class="step-btn step-btn-plus" @click.stop="onAdd">
            <text>+</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { formatPrice } from '@/utils'

const props = defineProps({
  item: { type: Object, required: true },
  count: { type: Number, default: 0 }
})

const emit = defineEmits(['add', 'subtract', 'choose-flavor', 'click'])

const defaultImage = '/static/images/default-dish.png'
const hasFlavors = computed(() => props.item.flavors && props.item.flavors.length > 0)
const flavorCount = computed(() => {
  // 同菜品不同口味的总数
  return props.count
})

function onAdd() {
  if (props.item.status === 0) {
    uni.showToast({ title: '该菜品已停售', icon: 'none' })
    return
  }
  emit('add', props.item)
}

function onSubtract() {
  emit('subtract', props.item)
}

function onChooseFlavor() {
  if (props.item.status === 0) {
    uni.showToast({ title: '该菜品已停售', icon: 'none' })
    return
  }
  emit('choose-flavor', props.item)
}

function onClick() {
  emit('click', props.item)
}
</script>

<style lang="scss" scoped>
.dish-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin: 0 var(--space-4) var(--space-4);
  box-shadow: var(--shadow-md);
  transition: transform 0.15s, box-shadow 0.2s;

  &:active {
    transform: scale(0.99);
    box-shadow: var(--shadow-sm);
  }

  .dish-image-wrapper {
    position: relative;
    width: 100%;
    // 16:10 比例，更适合餐饮图片
    padding-top: 62.5%;
    overflow: hidden;
    background: var(--color-bg-muted);

    .dish-image {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .dish-mask {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;

      .dish-mask-text {
        color: #FFFFFF;
        font-size: var(--text-sm);
        font-weight: var(--font-semibold);
        background: rgba(0, 0, 0, 0.4);
        padding: var(--space-2) var(--space-4);
        border-radius: var(--radius-full);
        letter-spacing: 1rpx;
      }
    }

    .dish-badge {
      position: absolute;
      top: var(--space-3);
      left: var(--space-3);
      background: var(--color-primary);
      color: #FFFFFF;
      font-size: var(--text-xs);
      font-weight: var(--font-semibold);
      padding: var(--space-1) var(--space-3);
      border-radius: var(--radius-sm);
      letter-spacing: 0.5rpx;
    }
  }

  .dish-info {
    padding: var(--space-4);
    display: flex;
    flex-direction: column;

    .dish-name {
      font-size: var(--text-lg);
      font-weight: var(--font-bold);
      color: var(--color-text-primary);
      line-height: 1.3;
      letter-spacing: 0.5rpx;
    }

    .dish-desc {
      font-size: var(--text-sm);
      color: var(--color-text-tertiary);
      margin-top: var(--space-2);
      line-height: 1.4;
    }

    .dish-meta {
      display: flex;
      align-items: center;
      margin-top: var(--space-3);
      gap: var(--space-4);

      .dish-sales,
      .dish-praise {
        font-size: var(--text-xs);
        color: var(--color-text-tertiary);
        line-height: 1.4;
      }
    }

    .dish-bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: var(--space-4);

      .price {
        color: var(--color-accent);
        font-weight: var(--font-bold);
        display: flex;
        align-items: baseline;

        .symbol {
          font-size: var(--text-sm);
          margin-right: 2rpx;
          font-weight: var(--font-semibold);
        }

        .value {
          font-size: var(--text-2xl);
          letter-spacing: -0.5rpx;
        }
      }

      .dish-action {
        display: flex;
        align-items: center;

        .btn-spec {
          position: relative;
          background: var(--color-primary);
          color: #FFFFFF;
          font-size: var(--text-sm);
          font-weight: var(--font-semibold);
          padding: var(--space-2) var(--space-4);
          border-radius: var(--radius-full);
          box-shadow: 0 2rpx 8rpx rgba(232, 93, 58, 0.2);
          transition: transform 0.1s;

          &:active {
            transform: scale(0.95);
          }

          .spec-count {
            position: absolute;
            top: -12rpx;
            right: -8rpx;
            background: var(--color-accent);
            color: #FFFFFF;
            font-size: var(--text-xs);
            min-width: 30rpx;
            height: 30rpx;
            line-height: 30rpx;
            text-align: center;
            border-radius: var(--radius-full);
            padding: 0 var(--space-2);
            font-weight: var(--font-semibold);
            border: 2rpx solid var(--color-bg-card);
          }
        }
      }
    }
  }
}
</style>
