<template>
  <view class="index-page">
    <!-- 顶部品牌区 -->
    <view class="brand-header">
      <view class="brand-info">
        <view class="brand-greeting">今天想吃点什么？</view>
        <view class="brand-sub">新鲜速达 · 美味到家</view>
      </view>
      <view class="shop-status" :class="{ closed: shopStatus === 0 }">
        <text class="status-dot" />
        <text class="status-text">{{ shopStatus === 1 ? '营业中' : '已打烊' }}</text>
      </view>
    </view>

    <!-- 搜索框 -->
    <view class="search-section">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          placeholder="搜索菜品"
          placeholder-class="search-placeholder"
          disabled
        />
      </view>
    </view>

    <!-- 主体：左侧分类 + 右侧菜品 -->
    <view class="main-content">
      <scroll-view scroll-y class="category-sidebar">
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="category-item"
          :class="{ active: activeCategoryId === cat.id }"
          @click="switchCategory(cat.id)"
        >
          <text>{{ cat.name }}</text>
        </view>
      </scroll-view>

      <scroll-view scroll-y class="dish-list">
        <!-- 当前分类标题 -->
        <view class="category-header">
          <text class="category-name">{{ activeCategoryName }}</text>
          <text class="category-count">{{ currentDishes.length }}道菜</text>
        </view>

        <!-- 猜你喜欢 -->
        <view v-if="recommendList.length > 0" class="recommend-section">
          <view class="recommend-title">
            <text class="rec-icon">💛</text>
            <text>猜你喜欢</text>
          </view>
          <scroll-view scroll-x class="recommend-scroll">
            <view
              v-for="item in recommendList"
              :key="'rec-' + item.id"
              class="recommend-card"
              @click="goDishDetail(item)"
            >
              <image class="recommend-img" :src="item.image || defaultImage" mode="aspectFill" />
              <view class="recommend-info">
                <view class="recommend-name text-ellipsis">{{ item.name }}</view>
                <view class="price price-small">
                  <text class="symbol">¥</text>
                  <text class="value-small">{{ formatPrice(item.price) }}</text>
                </view>
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 菜品列表 -->
        <view class="dish-list-inner">
          <dish-card
            v-for="dish in currentDishes"
            :key="dish.id"
            :item="dish"
            :count="getDishCount(dish)"
            @add="onAddDish"
            @subtract="onSubDish"
            @click="goDishDetail"
            @choose-flavor="openFlavorPicker"
          />
        </view>

        <view v-if="currentDishes.length === 0" class="empty-state">
          <view class="empty-icon">🍽️</view>
          <view class="empty-text">该分类暂无菜品</view>
        </view>
      </scroll-view>
    </view>

    <!-- 购物车悬浮条 -->
    <view class="cart-bar" v-if="shopStatus === 1">
      <view class="cart-bar-content" @click="goCart">
        <view class="cart-icon-wrapper">
          <text class="cart-icon">🛒</text>
          <view v-if="cartStore.totalCount > 0" class="cart-badge">{{ cartStore.totalCount }}</view>
        </view>
        <view class="cart-info">
          <view class="price">
            <text class="symbol">¥</text>
            <text class="value">{{ cartStore.totalAmount }}</text>
          </view>
          <view class="cart-delivery">配送费¥{{ deliveryFee }}</view>
        </view>
      </view>
      <view class="cart-btn" :class="{ disabled: cartStore.totalCount === 0 }" @click="goCheckout">
        去结算
      </view>
    </view>

    <!-- 口味选择器 -->
    <flavor-picker
      :visible="flavorPickerVisible"
      :dish="currentDish"
      @update:visible="flavorPickerVisible = $event"
      @confirm="onFlavorConfirm"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useCartStore } from '@/stores/cart'
import { getShopStatus } from '@/api/shop'
import { getCategoryList, getDishList } from '@/api/goods'
import { getRecommendList } from '@/api/cart'
import DishCard from '@/components/DishCard.vue'
import FlavorPicker from '@/components/FlavorPicker.vue'
import { formatPrice } from '@/utils'

const cartStore = useCartStore()
const defaultImage = '/static/images/default-dish.png'
const deliveryFee = ref(5)

const shopStatus = ref(1)
const categories = ref([])
const dishesMap = ref({})
const activeCategoryId = ref(null)
const recommendList = ref([])

const flavorPickerVisible = ref(false)
const currentDish = ref({})

const activeCategoryName = computed(() => {
  const cat = categories.value.find(c => c.id === activeCategoryId.value)
  return cat ? cat.name : ''
})

const currentDishes = computed(() => {
  return dishesMap.value[activeCategoryId.value] || []
})

async function fetchShopStatus() {
  try {
    shopStatus.value = await getShopStatus()
  } catch (e) {
    shopStatus.value = 1
  }
}

async function fetchCategories() {
  try {
    const data = await getCategoryList(1)
    categories.value = data || []
    if (categories.value.length > 0) {
      activeCategoryId.value = categories.value[0].id
      await fetchDishes(activeCategoryId.value)
    }
  } catch (e) {
    console.log('获取分类失败', e)
  }
}

async function fetchDishes(categoryId) {
  if (dishesMap.value[categoryId]) return
  try {
    const data = await getDishList(categoryId)
    dishesMap.value[categoryId] = data || []
  } catch (e) {
    dishesMap.value[categoryId] = []
  }
}

async function fetchRecommend() {
  try {
    recommendList.value = await getRecommendList()
  } catch (e) {
    recommendList.value = []
  }
}

function switchCategory(id) {
  activeCategoryId.value = id
  fetchDishes(id)
}

function getDishCount(dish) {
  return cartStore.items
    .filter(i => i.dishId === dish.id)
    .reduce((sum, i) => sum + i.number, 0)
}

function onAddDish(dish) {
  cartStore.add(dish.id, null, null)
}

function onSubDish(dish) {
  cartStore.sub(dish.id, null, null)
}

function openFlavorPicker(dish) {
  currentDish.value = dish
  flavorPickerVisible.value = true
}

function onFlavorConfirm(flavorStr) {
  cartStore.add(currentDish.value.id, null, flavorStr)
}

function goDishDetail(dish) {
  uni.navigateTo({ url: `/pages/dish-detail/dish-detail?id=${dish.id}&data=${encodeURIComponent(JSON.stringify(dish))}` })
}

function goCart() {
  if (cartStore.totalCount === 0) {
    uni.showToast({ title: '购物车是空的', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/cart/cart' })
}

function goCheckout() {
  if (cartStore.totalCount === 0) return
  uni.navigateTo({ url: '/pages/checkout/checkout' })
}

onMounted(() => {
  fetchShopStatus()
  fetchCategories()
  fetchRecommend()
  cartStore.fetchCart()
})

onShow(() => {
  cartStore.fetchCart()
  fetchShopStatus()
})
</script>

<style lang="scss" scoped>
.index-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg-page);
}

/* ===== 品牌头部 ===== */
.brand-header {
  background: var(--color-bg-card);
  padding: var(--space-6);
  padding-top: calc(var(--space-6) + env(safe-area-inset-top));
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid var(--color-divider);

  .brand-info {
    .brand-greeting {
      font-size: var(--text-xl);
      font-weight: var(--font-bold);
      color: var(--color-text-primary);
      letter-spacing: 0.5rpx;
      line-height: 1.3;
    }
    .brand-sub {
      font-size: var(--text-sm);
      color: var(--color-text-tertiary);
      margin-top: var(--space-1);
    }
  }

  .shop-status {
    display: flex;
    align-items: center;
    background: var(--color-primary-soft);
    padding: var(--space-2) var(--space-4);
    border-radius: var(--radius-full);

    .status-dot {
      width: 12rpx;
      height: 12rpx;
      border-radius: 50%;
      background: var(--color-success);
      margin-right: var(--space-2);
    }

    .status-text {
      font-size: var(--text-xs);
      color: var(--color-text-secondary);
      font-weight: var(--font-medium);
    }

    &.closed {
      .status-dot { background: var(--color-text-disabled); }
    }
  }
}

/* ===== 搜索区 ===== */
.search-section {
  padding: var(--space-4) var(--space-6);
  background: var(--color-bg-card);

  .search-box {
    display: flex;
    align-items: center;
    background: var(--color-bg-muted);
    border-radius: var(--radius-full);
    padding: var(--space-3) var(--space-4);

    .search-icon {
      font-size: var(--text-base);
      margin-right: var(--space-3);
      opacity: 0.5;
    }

    .search-input {
      flex: 1;
      font-size: var(--text-base);
      color: var(--color-text-primary);
    }

    .search-placeholder {
      color: var(--color-text-tertiary);
    }
  }
}

/* ===== 主体内容 ===== */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧分类 */
.category-sidebar {
  width: 170rpx;
  background: var(--color-bg-muted);
  flex-shrink: 0;

  .category-item {
    padding: var(--space-8) var(--space-4);
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
    text-align: center;
    position: relative;
    transition: all 0.2s;
    font-weight: var(--font-normal);

    &.active {
      background: var(--color-bg-card);
      color: var(--color-primary);
      font-weight: var(--font-semibold);
      font-size: var(--text-base);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 6rpx;
        height: 40rpx;
        background: var(--color-primary);
        border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
      }
    }
  }
}

/* 右侧菜品列表 */
.dish-list {
  flex: 1;
  background: var(--color-bg-page);

  .category-header {
    display: flex;
    align-items: baseline;
    padding: var(--space-6) var(--space-6) var(--space-4);

    .category-name {
      font-size: var(--text-lg);
      font-weight: var(--font-bold);
      color: var(--color-text-primary);
    }

    .category-count {
      font-size: var(--text-sm);
      color: var(--color-text-tertiary);
      margin-left: var(--space-3);
    }
  }
}

/* 猜你喜欢 */
.recommend-section {
  padding: 0 var(--space-6) var(--space-5);

  .recommend-title {
    display: flex;
    align-items: center;
    font-size: var(--text-base);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin-bottom: var(--space-4);

    .rec-icon {
      margin-right: var(--space-2);
    }
  }

  .recommend-scroll {
    white-space: nowrap;

    .recommend-card {
      display: inline-block;
      width: 200rpx;
      margin-right: var(--space-4);
      background: var(--color-bg-card);
      border-radius: var(--radius-lg);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
      vertical-align: top;

      &:last-child {
        margin-right: 0;
      }

      .recommend-img {
        width: 100%;
        height: 140rpx;
        object-fit: cover;
      }

      .recommend-info {
        padding: var(--space-3);

        .recommend-name {
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          font-weight: var(--font-medium);
        }

        .price {
          margin-top: var(--space-2);
          color: var(--color-accent);

          .symbol {
            font-size: var(--text-xs);
          }

          .value-small {
            font-size: var(--text-base);
          }
        }
      }
    }
  }
}

/* 菜品列表 */
.dish-list-inner {
  padding-bottom: 180rpx;
}

/* ===== 购物车悬浮条 ===== */
.cart-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--color-bg-card);
  display: flex;
  align-items: center;
  padding: var(--space-3) var(--space-6);
  padding-bottom: calc(var(--space-3) + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
  border-top: 1rpx solid var(--color-divider);

  .cart-bar-content {
    flex: 1;
    display: flex;
    align-items: center;

    .cart-icon-wrapper {
      position: relative;
      width: 80rpx;
      height: 80rpx;
      background: var(--color-bg-muted);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: var(--space-4);

      .cart-icon {
        font-size: 40rpx;
      }

      .cart-badge {
        position: absolute;
        top: -4rpx;
        right: -4rpx;
        background: var(--color-accent);
        color: #FFFFFF;
        font-size: var(--text-xs);
        min-width: 32rpx;
        height: 32rpx;
        line-height: 32rpx;
        text-align: center;
        border-radius: var(--radius-full);
        padding: 0 var(--space-2);
        font-weight: var(--font-semibold);
        border: 2rpx solid var(--color-bg-card);
      }
    }

    .cart-info {
      .price {
        color: var(--color-accent);
        .symbol { font-size: var(--text-sm); }
        .value { font-size: var(--text-2xl); }
      }
      .cart-delivery {
        color: var(--color-text-tertiary);
        font-size: var(--text-xs);
        margin-top: 2rpx;
      }
    }
  }

  .cart-btn {
    background: var(--color-primary);
    color: #FFFFFF;
    font-weight: var(--font-semibold);
    padding: var(--space-4) var(--space-8);
    border-radius: var(--radius-full);
    font-size: var(--text-base);
    box-shadow: 0 4rpx 12rpx rgba(232, 93, 58, 0.25);

    &.disabled {
      background: var(--color-text-disabled);
      color: #FFFFFF;
      box-shadow: none;
    }
  }
}
</style>
