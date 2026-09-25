<template>
  <view class="dish-detail-page">
    <image class="detail-image" :src="dish.image || defaultImage" mode="aspectFill" />

    <view class="detail-content">
      <view class="detail-name">{{ dish.name }}</view>
      <view class="detail-desc">{{ dish.description || '店家推荐，值得一试' }}</view>
      <view class="detail-price">
        <view class="price">
          <text class="symbol">¥</text>
          <text class="value">{{ formatPrice(dish.price) }}</text>
        </view>
        <!-- 有口味 -->
        <view v-if="hasFlavors" class="btn-spec" @click="openFlavor">选规格</view>
        <view v-else class="detail-stepper">
          <view v-if="count > 0" class="step-btn step-btn-minus" @click="onSub">−</view>
          <text v-if="count > 0" class="step-count">{{ count }}</text>
          <view class="step-btn step-btn-plus" @click="onAdd">+</view>
        </view>
      </view>
    </view>

    <!-- 口味选择器 -->
    <flavor-picker
      :visible="flavorPickerVisible"
      :dish="dish"
      @update:visible="flavorPickerVisible = $event"
      @confirm="onFlavorConfirm"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDishList } from '@/api/goods'
import { useCartStore } from '@/stores/cart'
import FlavorPicker from '@/components/FlavorPicker.vue'
import { formatPrice } from '@/utils'

const dish = ref({})
const dishId = ref(null)
const flavorPickerVisible = ref(false)
const cartStore = useCartStore()

const defaultImage = '/static/images/default-dish.png'
const hasFlavors = computed(() => dish.value.flavors && dish.value.flavors.length > 0)

const count = computed(() => {
  return cartStore.items
    .filter(i => i.dishId === dish.value.id)
    .reduce((sum, i) => sum + i.number, 0)
})

async function fetchDish() {
  // 菜品详情：重新从列表找（或后续增加详情接口）
  // 这里简化处理：从分类菜品列表中查找
  dish.value = { id: dishId.value, name: '加载中...' }
}

function openFlavor() {
  flavorPickerVisible.value = true
}

function onAdd() {
  cartStore.add(dish.value.id, null, null)
}

function onSub() {
  cartStore.sub(dish.value.id, null, null)
}

function onFlavorConfirm(flavorStr) {
  cartStore.add(dish.value.id, null, flavorStr)
}

onMounted((options) => {
  if (options) {
    dishId.value = options.id
    dish.value = JSON.parse(decodeURIComponent(options.data || '{}'))
    if (!dish.value || !dish.value.id) {
      fetchDish()
    }
  }
})
</script>

<style lang="scss" scoped>
.dish-detail-page {
  min-height: 100vh;
  background: var(--sky-page);
}

.detail-image {
  width: 100%;
  height: 500rpx;
}

.detail-content {
  background: var(--sky-card);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  margin-top: -32rpx;
  position: relative;
  padding: 32rpx;
  min-height: 400rpx;

  .detail-name {
    font-size: 36rpx;
    font-weight: 700;
    color: var(--sky-title);
  }

  .detail-desc {
    font-size: 26rpx;
    color: var(--sky-muted);
    margin-top: 12rpx;
    line-height: 1.5;
  }

  .detail-price {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 32rpx;

    .btn-spec {
      background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
      color: #2B2B2B;
      font-size: 26rpx;
      font-weight: 700;
      padding: 16rpx 40rpx;
      border-radius: 40rpx;
      box-shadow: 0 4rpx 12rpx rgba(255, 195, 0, 0.4);
    }

    .detail-stepper {
      display: flex;
      align-items: center;
    }
  }
}
</style>
