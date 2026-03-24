<template>
  <view class="chat-page">
    <view class="chat-header">
      <view class="header-left">
        <text class="header-title">AI智能问答</text>
        <text class="header-subtitle">基于文物知识的智能助手</text>
      </view>
      <view class="header-actions">
        <view class="action-btn" @click="handleClearHistory">
          <image class="action-icon" :src="trashIcon" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <scroll-view
      class="chat-content"
      scroll-y="true"
      :scroll-into-view="scrollToView"
      :scroll-with-animation="true"
    >
      <view class="chat-list" id="chatList">
        <view
          class="chat-item"
          :id="'msg-' + index"
          :class="msg.type"
          v-for="(msg, index) in chatHistory"
          :key="index"
        >
          <view class="chat-avatar" v-if="msg.type === 'ai'">
            <image class="avatar-image" :src="aiAvatarImage" mode="aspectFill"></image>
          </view>
          <view class="chat-message">
            <view class="message-content">
              <text class="message-text" v-if="msg.type === 'user'">{{ msg.content }}</text>
              <rich-text class="message-text rich-text" v-else :nodes="formatMessage(msg.content)"></rich-text>
            </view>
            <text class="message-time">{{ formatTime(msg.timestamp) }}</text>
          </view>
          <view class="chat-avatar" v-if="msg.type === 'user'">
            <image class="avatar-image" :src="userInfo?.avatar || defaultAvatarImage" mode="aspectFill"></image>
          </view>
        </view>

        <view class="chat-item ai" v-if="isLoading">
          <view class="chat-avatar">
            <image class="avatar-image" :src="aiAvatarImage" mode="aspectFill"></image>
          </view>
          <view class="chat-message">
            <view class="message-content loading">
              <view class="typing-indicator">
                <view class="dot"></view>
                <view class="dot"></view>
                <view class="dot"></view>
              </view>
            </view>
          </view>
        </view>

        <EmptyState
          v-if="chatHistory.length === 0 && !isLoading"
          :image="emptyChatImage"
          text="开始与AI对话吧"
          :show-action="true"
        >
          <view class="quick-questions">
            <text
              class="question-item"
              v-for="(question, index) in quickQuestions"
              :key="index"
              @click="handleQuickQuestion(question)"
            >
              {{ question }}
            </text>
          </view>
        </EmptyState>
      </view>
    </scroll-view>

    <view class="chat-input-bar">
      <view class="input-wrapper">
        <input
          class="chat-input"
          type="text"
          v-model="inputMessage"
          placeholder="输入您的问题..."
          :disabled="isLoading"
          @confirm="handleSend"
        />
        <button class="send-btn" :class="{ disabled: !canSend }" @click="handleSend">
          <text class="send-text">发送</text>
        </button>
      </view>
    </view>

    <Loading :visible="false" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user.js'
import { useChatStore } from '@/stores/chat.js'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import Loading from '@/components/Loading/Loading.vue'
import { dialogApi } from '@/common/api/index.js'
import { formatDate, parseMarkdown } from '@/common/utils/index.js'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'

const userStore = useUserStore()
const chatStore = useChatStore()

const userInfo = computed(() => userStore.userInfo)
const chatHistory = computed(() => chatStore.chatHistory)
const isLoading = computed(() => chatStore.isLoading)

const inputMessage = ref('')
const scrollToView = ref('')

const artifactId = ref('')
const artifactName = ref('')
const PENDING_CHAT_ARTIFACT_KEY = 'pendingChatArtifactContext'
const aiAvatarImage = resolveAssetPath('/static/images/ai-avatar.png')
const defaultAvatarImage = resolveAssetPath('/static/images/default-avatar.png')
const emptyChatImage = resolveAssetPath('/static/images/empty-chat.png')
const trashIcon = resolveAssetPath('/static/icons/trash.svg')

onLoad((options = {}) => {
  artifactId.value = options.artifactId || ''
  artifactName.value = options.artifactName ? decodeURIComponent(options.artifactName) : ''
})

const applyPendingArtifactContext = () => {
  const pending = uni.getStorageSync(PENDING_CHAT_ARTIFACT_KEY)
  if (!pending) return

  artifactId.value = pending.artifactId ? String(pending.artifactId) : ''
  artifactName.value = pending.artifactName || ''

  // 用户从文物详情/识别结果进入问答时，开启一个新的会话上下文。
  chatStore.clearHistory()
  chatStore.setCurrentDialogId(null)

  if (artifactName.value) {
    inputMessage.value = `关于"${artifactName.value}"，我想了解更多`
  }

  uni.removeStorageSync(PENDING_CHAT_ARTIFACT_KEY)
}

onShow(() => {
  applyPendingArtifactContext()
})

const quickQuestions = ref([
  '这件文物有什么历史意义？',
  '这件文物的制作工艺是怎样的？',
  '这件文物属于哪个朝代？',
  '这件文物的特点是什么？'
])

onMounted(() => {
  chatStore.loadFromStorage()

  if (artifactName.value) {
    inputMessage.value = `关于"${artifactName.value}"，我想了解更多`
  }
})

watch(chatHistory, () => {
  scrollToBottom()
}, { deep: true })

const canSend = computed(() => {
  return inputMessage.value.trim().length > 0 && !isLoading.value
})

const handleSend = async () => {
  if (!canSend.value) return

  const message = inputMessage.value.trim()
  inputMessage.value = ''

  chatStore.addMessage({
    type: 'user',
    content: message,
    timestamp: Date.now()
  })

  chatStore.setLoading(true)

  try {
    const res = await dialogApi.createRequest({
      query: message,
      artifactId: artifactId.value ? Number(artifactId.value) : undefined,
      contextHistory: chatHistory.value.map(msg => ({
        role: msg.type === 'user' ? 'user' : 'assistant',
        content: msg.content
      }))
    })

    const dialogId = res.data.dialogId
    chatStore.setCurrentDialogId(dialogId)

    let aiResponse = res.data.aiResponse
    if (!aiResponse && dialogId) {
      const result = await dialogApi.getResult(dialogId)
      aiResponse = result.data.aiResponse
    }

    chatStore.addMessage({
      type: 'ai',
      content: aiResponse || '暂未获取到回复，请稍后重试。',
      timestamp: Date.now()
    })
  } catch (error) {
    console.error('发送消息失败:', error)
    uni.showToast({
      title: error.message || '发送失败，请重试',
      icon: 'none'
    })
  } finally {
    chatStore.setLoading(false)
  }
}

const handleQuickQuestion = (question) => {
  inputMessage.value = question
  handleSend()
}

const handleClearHistory = async () => {
  uni.showModal({
    title: '提示',
    content: '确定要清空对话历史吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await dialogApi.clearHistories(artifactId.value)
          chatStore.clearHistory()
          uni.showToast({
            title: '已清空',
            icon: 'success'
          })
        } catch (error) {
          console.error('清空历史失败:', error)
          chatStore.clearHistory()
          uni.showToast({
            title: '已清空',
            icon: 'success'
          })
        }
      }
    }
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistory.value.length > 0) {
      scrollToView.value = 'msg-' + (chatHistory.value.length - 1)
    }
  })
}

const formatTime = (timestamp) => {
  return formatDate(timestamp, 'HH:mm')
}

const formatMessage = (content) => {
  return parseMarkdown(content)
}
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .chat-header {
    background: #FFF8DC;
    padding: 24rpx 32rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 2rpx 8rpx rgba(139, 69, 19, 0.1);
    border-bottom: 2rpx solid #D2B48C;

    .header-left {
      .header-title {
        display: block;
        font-size: 32rpx;
        color: #5D4037;
        font-weight: 600;
        margin-bottom: 8rpx;
        font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
      }

      .header-subtitle {
        display: block;
        font-size: 24rpx;
        color: #8B7355;
      }
    }

    .header-actions {
      .action-btn {
        width: 64rpx;
        height: 64rpx;
        background: #FAF0E6;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 2rpx solid #D2B48C;

        .action-icon {
          width: 32rpx;
          height: 32rpx;
        }
      }
    }
  }

  .chat-content {
    flex: 1;
    overflow: hidden;

    .chat-list {
      padding: 24rpx;

      .chat-item {
        display: flex;
        margin-bottom: 32rpx;
        gap: 20rpx;

        &.user {
          flex-direction: row-reverse;

          .chat-message {
            align-items: flex-end;

            .message-content {
              background: #8B4513;
              color: #FFF8DC;
              border: 2rpx solid #65320F;

              .message-text {
                color: #FFF8DC;
              }
            }

            .message-time {
              text-align: right;
            }
          }
        }

        &.ai {
          .chat-message {
            align-items: flex-start;

            .message-content {
              background: #FFF8DC;
              color: #5D4037;
              border: 2rpx solid #D2B48C;

              .message-text {
                color: #5D4037;
              }

              &.loading {
                padding: 24rpx 32rpx;
              }
            }
          }
        }

        .chat-avatar {
          flex-shrink: 0;

          .avatar-image {
            width: 72rpx;
            height: 72rpx;
            border-radius: 50%;
          }
        }

        .chat-message {
          flex: 1;
          display: flex;
          flex-direction: column;
          max-width: calc(100% - 160rpx);

          .message-content {
            padding: 24rpx 28rpx;
            border-radius: 16rpx;
            margin-bottom: 8rpx;
            word-wrap: break-word;

            .message-text {
              font-size: 28rpx;
              line-height: 1.6;

              &.rich-text {
                :deep(img) {
                  max-width: 100%;
                }
              }
            }

            .typing-indicator {
              display: flex;
              gap: 8rpx;
              align-items: center;

              .dot {
                width: 12rpx;
                height: 12rpx;
                background: #999;
                border-radius: 50%;
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
            font-size: 20rpx;
            color: #8B7355;
          }
        }
      }

      .quick-questions {
        display: flex;
        flex-direction: column;
        gap: 16rpx;
        width: 100%;

        .question-item {
          background: #FFF8DC;
          padding: 24rpx 32rpx;
          border-radius: 12rpx;
          font-size: 28rpx;
          color: #5D4037;
          box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
          border: 2rpx solid #D2B48C;
          transition: all 0.3s;

          &:active {
            transform: scale(0.98);
            background: #FAF0E6;
          }
        }
      }
    }
  }

  .chat-input-bar {
    background: #FFF8DC;
    padding: 24rpx 32rpx;
    padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
    box-shadow: 0 -2rpx 8rpx rgba(139, 69, 19, 0.1);
    border-top: 2rpx solid #D2B48C;

    .input-wrapper {
      display: flex;
      gap: 16rpx;
      align-items: center;

      .chat-input {
        flex: 1;
        height: 80rpx;
        background: #FAF0E6;
        border-radius: 40rpx;
        padding: 0 32rpx;
        font-size: 28rpx;
        color: #5D4037;
        border: 2rpx solid #D2B48C;

        &::placeholder {
          color: #8B7355;
        }

        &[disabled] {
          opacity: 0.6;
        }
      }

      .send-btn {
        width: 160rpx;
        height: 80rpx;
        background: #8B4513;
        border-radius: 40rpx;
        color: #FFF8DC;
        font-size: 28rpx;
        font-weight: 600;
        border: 2rpx solid #8B4513;
        display: flex;
        align-items: center;
        justify-content: center;

        &.disabled {
          background: #D2B48C;
          border-color: #D2B48C;
        }
      }
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8rpx);
  }
}
</style>