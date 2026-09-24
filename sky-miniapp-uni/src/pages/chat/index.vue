<template>
  <view class="chat-page">
    <scroll-view scroll-y class="chat-list" :scroll-into-view="scrollToMsg">
      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        :id="`msg-${idx}`"
        class="msg-row"
        :class="msg.role === 'user' ? 'msg-right' : 'msg-left'"
      >
        <view v-if="msg.role === 'assistant'" class="avatar service-avatar">
          <uni-icons type="headphones" size="18" color="#fff" />
        </view>
        <view class="msg-bubble" :class="msg.role">
          <text>{{ msg.content }}</text>
        </view>
        <view v-if="msg.role === 'user'" class="avatar user-avatar">
          <uni-icons type="person" size="18" color="#fff" />
        </view>
      </view>
      <view v-if="loading" class="msg-row msg-left">
        <view class="avatar service-avatar">
          <uni-icons type="headphones" size="18" color="#fff" />
        </view>
        <view class="msg-bubble assistant typing">
          <text class="dot"></text>
          <text class="dot"></text>
          <text class="dot"></text>
        </view>
      </view>
    </scroll-view>

    <!-- 输入区 -->
    <view class="input-bar">
      <input
        class="chat-input"
        v-model="inputMsg"
        placeholder="输入您的问题..."
        placeholder-style="color:#ccc"
        confirm-type="send"
        @confirm="handleSend"
      />
      <view class="send-btn" :class="{ disabled: !inputMsg.trim() || loading }" @click="handleSend">
        <uni-icons type="paperplane-filled" size="20" color="#fff" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'

const STORAGE_KEY = 'chat_history'
const MAX_HISTORY = 50

const messages = ref([])
const inputMsg = ref('')
const loading = ref(false)
const scrollToMsg = ref('')

const loadHistory = () => {
  const history = uni.getStorageSync(STORAGE_KEY) || []
  messages.value = history
}

const saveHistory = () => {
  const toSave = messages.value.slice(-MAX_HISTORY)
  uni.setStorageSync(STORAGE_KEY, toSave)
}

const handleSend = async () => {
  const text = inputMsg.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputMsg.value = ''
  loading.value = true
  scrollToMsg.value = `msg-${messages.value.length - 1}`

  try {
    const reply = await request('/user/chat', 'POST', { message: text })
    messages.value.push({ role: 'assistant', content: reply.data || reply || '抱歉，暂时无法回答' })
    saveHistory()
    setTimeout(() => {
      scrollToMsg.value = `msg-${messages.value.length - 1}`
    }, 100)
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，客服暂时无法回答，请稍后再试。' })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.chat-list {
  flex: 1;
  padding: 20rpx;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 30rpx;
}

.msg-right {
  flex-direction: row-reverse;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.service-avatar {
  background-color: #FFC300;
}

.user-avatar {
  background-color: #1890ff;
}

.msg-bubble {
  max-width: 500rpx;
  padding: 20rpx 28rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-all;
}

.msg-bubble.assistant {
  background-color: #fff;
  color: #333;
  margin-left: 16rpx;
  border-top-left-radius: 4rpx;
}

.msg-bubble.user {
  background-color: #FFC300;
  color: #fff;
  margin-right: 16rpx;
  border-top-right-radius: 4rpx;
}

.typing {
  display: flex;
  align-items: center;
  gap: 8rpx;

  .dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background-color: #ccc;
    animation: typing 1.4s infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; }
  30% { opacity: 1; }
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 20rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  border-top: 1rpx solid #eee;
}

.chat-input {
  flex: 1;
  background-color: #f5f5f5;
  border-radius: 50rpx;
  padding: 16rpx 28rpx;
  font-size: 28rpx;
  color: #333;
}

.send-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background-color: #FFC300;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 16rpx;

  &.disabled {
    background-color: #ddd;
  }
}
</style>
