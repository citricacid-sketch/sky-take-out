<template>
  <view class="menu-page">
    <view class="menu-container">
      <!-- 左侧分类 -->
      <scroll-view scroll-y class="category-sidebar">
        <view
          class="cat-item"
          :class="{ active: activeCategory === cat.id }"
          v-for="cat in categories"
          :key="cat.id"
          @click="selectCategory(cat.id)"
        >
          <text>{{ cat.name }}</text>
        </view>
      </scroll-view>

      <!-- 右侧菜品 -->
      <scroll-view scroll-y class="dish-list">
        <view class="dish-title">{{ currentCatName }}</view>
        <dish-card
          v-for="dish in dishes"
          :key="dish.id"
          :dish="dish"
          @click="openFlavor(dish)"
          @add="openFlavor(dish)"
        />
        <view v-if="dishes.length === 0" class="empty-tip">
          <text>该分类暂无菜品</text>
        </view>
      </scroll-view>
    </view>

    <!-- 底部购物车栏 -->
    <view class="cart-bar">
      <view class="cart-info" @click="showCartPopup = true">
        <view class="cart-icon-wrap">
          <uni-icons type="cart-filled" size="28" color="#fff" />
          <view v-if="cartStore.totalQuantity > 0" class="cart-badge">
            {{ cartStore.totalQuantity }}
          </view>
        </view>
        <view class="cart-text">
          <text class="cart-total">¥{{ cartStore.totalAmount.toFixed(2) }}</text>
          <text class="cart-delivery">另需配送费 ¥{{ deliveryFee }}</text>
        </view>
      </view>
      <view class="settle-btn" :class="{ disabled: cartStore.isEmpty }" @click="handleSettle">
        去结算
      </view>
    </view>

    <!-- 购物车弹窗 -->
    <cart-popup
      v-model:visible="showCartPopup"
      @checkout="handleSettle"
    />

    <!-- 口味选择弹窗 -->
    <flavor-picker
      v-model:visible="showFlavor"
      :dish="currentDish"
      @confirm="handleAddToCart"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DishCard from '../../components/dish-card.vue'
import CartPopup from '../../components/cart-popup.vue'
import FlavorPicker from '../../components/flavor-picker.vue'
import { useCartStore } from '../../stores/cart'
import request from '../../utils/request'

const cartStore = useCartStore()
const categories = ref([])
const dishes = ref([])
const activeCategory = ref(null)
const showCartPopup = ref(false)
const showFlavor = ref(false)
const currentDish = ref({})
const deliveryFee = ref(5)

const currentCatName = computed(() => {
  const cat = categories.value.find(c => c.id === activeCategory.value)
  return cat ? cat.name : '全部菜品'
})

const loadCategories = async () => {
  try {
    const data = await request('/user/category/list?type=1', 'GET')
    categories.value = data || []
    if (data && data.length > 0) {
      activeCategory.value = data[0].id
      loadDishes(data[0].id)
    }
  } catch (e) {
    categories.value = []
  }
}

const loadDishes = async (categoryId) => {
  try {
    const data = await request(`/user/dish/list?categoryId=${categoryId}`, 'GET')
    dishes.value = (data || []).map(d => ({
      ...d,
      sales: d.sales || Math.floor(Math.random() * 200)
    }))
  } catch (e) {
    dishes.value = []
  }
}

const selectCategory = (id) => {
  activeCategory.value = id
  loadDishes(id)
}

const openFlavor = (dish) => {
  currentDish.value = dish
  showFlavor.value = true
}

const handleAddToCart = async (dish) => {
  await cartStore.addItem({
    dishId: dish.dishId || dish.id,
    setmealId: dish.setmealId,
    amount: dish.price,
    flavors: dish.flavors
  })
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

const handleSettle = () => {
  if (cartStore.isEmpty) {
    uni.showToast({ title: '购物车为空', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/order/confirm' })
}

onMounted(async () => {
  await loadCategories()
  await cartStore.loadCart()
})
</script>

<style lang="scss" scoped>
.menu-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.menu-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.category-sidebar {
  width: 180rpx;
  background-color: #f5f5f5;
  flex-shrink: 0;
}

.cat-item {
  padding: 30rpx 20rpx;
  font-size: 26rpx;
  color: #666;
  text-align: center;
  position: relative;

  &.active {
    background-color: #fff;
    color: #333;
    font-weight: 600;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 6rpx;
      height: 40rpx;
      background-color: #FFC300;
      border-radius: 0 6rpx 6rpx 0;
    }
  }
}

.dish-list {
  flex: 1;
  padding: 20rpx;
  background-color: #fff;
}

.dish-title {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 20rpx;
  padding: 10rpx 0;
}

.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}

.cart-bar {
  display: flex;
  align-items: center;
  background-color: #2A2A2A;
  padding: 16rpx 30rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}

.cart-info {
  flex: 1;
  display: flex;
  align-items: center;
}

.cart-icon-wrap {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: #FFC300;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.cart-badge {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  background-color: #FF4B33;
  color: #fff;
  font-size: 20rpx;
  min-width: 32rpx;
  height: 32rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;
}

.cart-text {
  display: flex;
  flex-direction: column;
}

.cart-total {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.cart-delivery {
  font-size: 22rpx;
  color: #999;
}

.settle-btn {
  background-color: #FFC300;
  color: #fff;
  padding: 20rpx 40rpx;
  border-radius: 50rpx;
  font-size: 28rpx;
  font-weight: 500;

  &.disabled {
    background-color: #555;
    color: #888;
  }
}
</style>
