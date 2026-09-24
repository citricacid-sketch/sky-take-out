<template>
  <div class="chat-panel">
    <div class="chat-header">
      <span class="chat-title">AI 数据助手</span>
      <el-button text :icon="Refresh" @click="clearMessages">清空</el-button>
    </div>
    <div class="chat-messages" ref="messageListRef">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="msg.role"
      >
        <el-avatar :size="32" :icon="msg.role === 'user' ? 'UserFilled' : 'ChatRound'" />
        <div class="message-content">
          <div class="message-text">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
      <div v-if="loading" class="message-item assistant">
        <el-avatar :size="32" icon="ChatRound" />
        <div class="message-content">
          <div class="message-text typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        placeholder="请输入您的问题，例如：今天的营业额是多少？"
        @keydown.enter.prevent="handleEnter"
        :disabled="loading"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="loading"
        @click="sendMessage"
        class="send-btn"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Refresh, Promotion } from '@element-plus/icons-vue'
import { formatDateTime } from '@/utils/format'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['send', 'clear'])

const inputText = ref('')
const messageListRef = ref(null)

function handleEnter(e) {
  if (!e.shiftKey) {
    sendMessage()
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  inputText.value = ''
}

function clearMessages() {
  emit('clear')
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

defineExpose({ scrollToBottom, formatDateTime })
</script>

<style lang="scss" scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;

  .chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;

    .chat-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .message-item {
      display: flex;
      gap: 12px;

      &.assistant {
        flex-direction: row;
      }

      &.user {
        flex-direction: row-reverse;

        .message-content {
          align-items: flex-end;
        }

        .message-text {
          background: #FFC300;
          color: #303133;
        }
      }

      .message-content {
        display: flex;
        flex-direction: column;
        gap: 4px;
        max-width: 70%;

        .message-text {
          padding: 10px 14px;
          background: #f4f4f5;
          border-radius: 8px;
          font-size: 14px;
          line-height: 1.6;
          color: #303133;
          white-space: pre-wrap;
          word-break: break-all;

          &.typing {
            span {
              display: inline-block;
              width: 6px;
              height: 6px;
              background: #909399;
              border-radius: 50%;
              margin: 0 2px;
              animation: typing 1.4s infinite;

              &:nth-child(2) {
                animation-delay: 0.2s;
              }
              &:nth-child(3) {
                animation-delay: 0.4s;
              }
            }
          }
        }

        .message-time {
          font-size: 12px;
          color: #c0c4cc;
        }
      }
    }
  }

  .chat-input {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #ebeef5;

    .send-btn {
      height: auto;
      align-self: flex-end;
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
}
</style>
