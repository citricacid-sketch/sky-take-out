<template>
  <view class="chat-page">
    <scroll-view scroll-y class="chat-list" :scroll-top="scrollTop">
      <view class="chat-tip">您好！我是苍穹外卖智能客服，有什么可以帮您？</view>

      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        class="message"
        :class="msg.role"
      >
        <view class="msg-avatar">
          <text>{{ msg.role === 'user' ? '👤' : '🤖' }}</text>
        </view>
        <view class="msg-bubble">
          <text>{{ msg.content }}</text>
        </view>
      </view>

      <view v-if="loading" class="message assistant">
        <view class="msg-avatar"><text>🤖</text></view>
        <view class="msg-bubble typing">
          <text class="dot">●</text>
          <text class="dot">●</text>
          <text class="dot">●</text>
        </view>
      </view>
    </scroll-view>

    <!-- 输入框 -->
    <view class="chat-input">
      <input
        class="input-field"
        v-model="inputText"
        placeholder="输入您的问题..."
        placeholder-style="color: #B8BFC9"
        confirm-type="send"
        @confirm="onSend"
      />
      <view class="send-btn" :class="{ disabled: !inputText.trim() }" @click="onSend">
        发送
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { chatWithAI } from '@/api/other'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const scrollTop = ref(0)

async function onSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true

  await nextTick()
  scrollTop.value = messages.value.length * 10000

  try {
    const reply = await chatWithAI(text)
    messages.value.push({ role: 'assistant', content: reply || '抱歉，我暂时无法回答这个问题。' })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，服务暂不可用，请稍后重试。' })
  } finally {
    loading.value = false
    await nextTick()
    scrollTop.value = messages.value.length * 10000
  }
}
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--sky-page);
}

.chat-list {
  flex: 1;
  padding: var(--space-m);
  overflow-y: auto;
}

.chat-tip {
  text-align: center;
  font-size: 22rpx;
  color: var(--sky-muted);
  background: rgba(255, 195, 0, 0.1);
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
  margin-bottom: 32rpx;
  display: inline-block;
  position: relative;
  left: 50%;
  transform: translateX(-50%);
}

.message {
  display: flex;
  margin-bottom: 32rpx;

  .msg-avatar {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    background: var(--sky-card);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    flex-shrink: 0;
    box-shadow: var(--shadow-card);
  }

  .msg-bubble {
    max-width: 70%;
    padding: 20rpx 28rpx;
    border-radius: var(--radius-m);
    font-size: 26rpx;
    line-height: 1.6;
    margin: 0 16rpx;
  }

  &.user {
    flex-direction: row-reverse;

    .msg-bubble {
      background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
      color: #2B2B2B;
      border-radius: var(--radius-m) var(--radius-s) var(--radius-m) var(--radius-m);
    }
  }

  &.assistant {
    .msg-bubble {
      background: var(--sky-card);
      color: var(--sky-body);
      border-radius: var(--radius-s) var(--radius-m) var(--radius-m) var(--radius-m);
      box-shadow: var(--shadow-card);
    }
  }
}

.typing {
  .dot {
    display: inline-block;
    margin: 0 4rpx;
    color: var(--sky-muted);
    animation: blink 1.4s infinite;
  }
  .dot:nth-child(2) { animation-delay: 0.2s; }
  .dot:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes blink {
  0%, 60%, 100% { opacity: 0.3; }
  30% { opacity: 1; }
}

.chat-input {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: var(--sky-card);
  border-top: 1rpx solid var(--sky-border);

  .input-field {
    flex: 1;
    background: var(--sky-page);
    border-radius: 40rpx;
    padding: 16rpx 28rpx;
    font-size: 26rpx;
    color: var(--sky-title);
  }

  .send-btn {
    margin-left: 16rpx;
    background: linear-gradient(135deg, var(--sky-primary), var(--sky-primary-dark));
    color: #2B2B2B;
    font-weight: 600;
    font-size: 26rpx;
    padding: 16rpx 32rpx;
    border-radius: 40rpx;

    &.disabled {
      background: #E0E0E0;
      color: #999;
    }
  }
}
</style>
