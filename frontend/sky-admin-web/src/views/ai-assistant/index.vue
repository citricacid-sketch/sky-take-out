<template>
  <div class="app-container">
    <div class="ai-container">
      <!-- 左侧对话面板 -->
      <div class="ai-chat">
        <ChatPanel
          :messages="messages"
          :loading="loading"
          @send="handleSend"
          @clear="handleClear"
          ref="chatPanelRef"
        />
      </div>

      <!-- 右侧信息面板 -->
      <div class="ai-side">
        <div class="side-card">
          <h3 class="side-title">使用提示</h3>
          <ul class="side-tips">
            <li>今天营业额是多少？</li>
            <li>最近一周的订单趋势</li>
            <li>销量最高的菜品有哪些？</li>
            <li>新增用户数量统计</li>
          </ul>
        </div>
        <div class="side-card">
          <h3 class="side-title">回答预览</h3>
          <div class="answer-preview">
            <div v-if="lastAnswer" class="answer-content">{{ lastAnswer }}</div>
            <el-empty v-else description="暂无回答，请在左侧提问" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiAsk } from '@/api/ai'
import ChatPanel from '@/components/ChatPanel.vue'

const messages = ref([])
const loading = ref(false)
const lastAnswer = ref('')
const chatPanelRef = ref(null)

async function handleSend(question) {
  messages.value.push({
    role: 'user',
    content: question,
    time: new Date().toLocaleTimeString(),
  })
  loading.value = true
  chatPanelRef.value?.scrollToBottom()

  try {
    const answer = await aiAsk(question)
    lastAnswer.value = answer
    messages.value.push({
      role: 'assistant',
      content: answer,
      time: new Date().toLocaleTimeString(),
    })
    chatPanelRef.value?.scrollToBottom()
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI 助手暂时无法回答，请稍后再试。',
      time: new Date().toLocaleTimeString(),
    })
  } finally {
    loading.value = false
  }
}

function handleClear() {
  messages.value = []
  lastAnswer.value = ''
  ElMessage.success('对话已清空')
}
</script>

<style lang="scss" scoped>
.ai-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 130px);

  .ai-chat {
    flex: 1;
    min-width: 0;
  }

  .ai-side {
    width: 360px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;

    .side-card {
      background: #fff;
      border-radius: 4px;
      padding: 16px;
      box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);

      .side-title {
        font-size: 16px;
        font-weight: 600;
        margin: 0 0 12px;
        color: #303133;
      }

      .side-tips {
        padding-left: 20px;

        li {
          padding: 4px 0;
          color: #606266;
          list-style: disc;
          font-size: 13px;
        }
      }

      .answer-preview {
        min-height: 150px;

        .answer-content {
          font-size: 14px;
          line-height: 1.8;
          color: #303133;
          white-space: pre-wrap;
        }
      }
    }
  }

  @media (max-width: 1100px) {
    flex-direction: column;

    .ai-side {
      width: 100%;
    }
  }
}
</style>
