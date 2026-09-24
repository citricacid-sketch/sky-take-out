<template>
  <view class="order-card" @click="handleClick">
    <view class="order-header">
      <text class="order-time">{{ order.orderTime || order.createTime }}</text>
      <text class="order-status" :class="statusClass">{{ statusText }}</text>
    </view>
    <view class="order-dishes">
      <view class="dish-row" v-for="(item, idx) in displayDishes" :key="idx">
        <text class="dish-name">{{ item.name }}</text>
        <text class="dish-count">x{{ item.number }}</text>
      </view>
      <text v-if="order.orderDetailList && order.orderDetailList.length > 2" class="more-tip">
        等 {{ order.orderDetailList.length }} 件
      </text>
    </view>
    <view class="order-footer">
      <text class="order-total">共{{ totalCount }}件，实付 ¥{{ order.amount }}</text>
      <view class="actions">
        <view
          v-for="action in actions"
          :key="action.action"
          class="action-btn"
          :class="action.action === 'PAY' ? 'btn-primary-action' : 'btn-secondary-action'"
          @click.stop="handleAction(action)"
        >
          {{ action.label }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../../utils/request'

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['action-click', 'click'])

const actions = ref([])

// 订单状态映射
const statusMap = {
  1: { text: '待付款', class: 'status-wait' },
  2: { text: '待接单', class: 'status-wait' },
  3: { text: '已接单', class: 'status-process' },
  4: { text: '派送中', class: 'status-process' },
  5: { text: '已完成', class: 'status-done' },
  6: { text: '已取消', class: 'status-cancel' }
}

const statusText = computed(() => statusMap[props.order.status]?.text || '未知')
const statusClass = computed(() => statusMap[props.order.status]?.class || '')

const displayDishes = computed(() => {
  const list = props.order.orderDetailList || []
  return list.slice(0, 2)
})

const totalCount = computed(() => {
  if (!props.order.orderDetailList) return 0
  return props.order.orderDetailList.reduce((sum, item) => sum + item.number, 0)
})

const fetchActions = async () => {
  try {
    const data = await request(`/user/order/${props.order.id}/actions`, 'GET')
    actions.value = data || []
  } catch (e) {
    actions.value = []
  }
}

const handleClick = () => {
  emit('click', props.order)
}

const handleAction = (action) => {
  emit('action-click', { order: props.order, action })
}

onMounted(() => {
  fetchActions()
})
</script>

<style lang="scss" scoped>
.order-card {
  background-color: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.order-time {
  font-size: 24rpx;
  color: #999;
}

.order-status {
  font-size: 26rpx;
  font-weight: 500;
}

.status-wait { color: #FF8C00; }
.status-process { color: #1890ff; }
.status-done { color: #52c41a; }
.status-cancel { color: #999; }

.order-dishes {
  border-bottom: 1rpx solid #f5f5f5;
  padding-bottom: 16rpx;
  margin-bottom: 16rpx;
}

.dish-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.dish-name {
  font-size: 28rpx;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-count {
  font-size: 26rpx;
  color: #666;
  margin-left: 20rpx;
}

.more-tip {
  font-size: 24rpx;
  color: #999;
}

.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.order-total {
  font-size: 26rpx;
  color: #666;
}

.actions {
  display: flex;
  gap: 16rpx;
}

.action-btn {
  padding: 12rpx 24rpx;
  border-radius: 50rpx;
  font-size: 24rpx;
  border: 1rpx solid transparent;
}

.btn-primary-action {
  background-color: #FFC300;
  color: #fff;
}

.btn-secondary-action {
  background-color: #fff;
  color: #666;
  border-color: #ddd;
}
</style>
