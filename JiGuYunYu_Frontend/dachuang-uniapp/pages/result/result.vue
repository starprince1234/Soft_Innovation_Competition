<template>
  <view class="result-page">
    <CustomNavbar title="识别结果" :show-back="true" />

    <view class="result-content">
      <view class="result-card" v-if="artifact">
        <view class="artifact-image">
          <image class="image" :src="artifact.imageUrl" mode="aspectFill" @click="previewImage"></image>
          <view class="confidence-badge">
            <text class="confidence-text">置信度: {{ artifact.confidence }}%</text>
          </view>
        </view>

        <view class="artifact-info">
          <view class="info-header">
            <text class="artifact-name">{{ artifact.name }}</text>
            <view class="artifact-tags">
              <text class="tag" v-for="(tag, index) in artifact.tags" :key="index">{{ tag }}</text>
            </view>
          </view>

          <view class="info-section">
            <text class="section-title">基本信息</text>
            <view class="info-grid">
              <view class="info-item">
                <text class="info-label">年代</text>
                <text class="info-value">{{ artifact.period || '未知' }}</text>
              </view>
              <view class="info-item">
                <text class="info-label">材质</text>
                <text class="info-value">{{ artifact.material || '未知' }}</text>
              </view>
              <view class="info-item">
                <text class="info-label">尺寸</text>
                <text class="info-value">{{ artifact.size || '未知' }}</text>
              </view>
              <view class="info-item">
                <text class="info-label">出土地</text>
                <text class="info-value">{{ artifact.location || '未知' }}</text>
              </view>
            </view>
          </view>

          <view class="info-section" v-if="artifact.description">
            <text class="section-title">文物简介</text>
            <text class="description-text">{{ artifact.description }}</text>
          </view>

          <view class="info-section" v-if="artifact.historicalSignificance">
            <text class="section-title">历史意义</text>
            <text class="description-text">{{ artifact.historicalSignificance }}</text>
          </view>
        </view>
      </view>

      <EmptyState v-else image="/static/images/empty-result.png" text="暂无识别结果" />

      <view class="action-buttons" v-if="artifact">
        <button class="action-btn secondary" @click="handleAskAI">
          <text class="btn-icon">🤖</text>
          <text class="btn-text">向AI提问</text>
        </button>
        <button class="action-btn primary" @click="handleShare">
          <text class="btn-icon">📤</text>
          <text class="btn-text">分享</text>
        </button>
      </view>
    </view>

    <Loading :visible="loading" text="加载中..." />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CustomNavbar from '@/components/CustomNavbar/CustomNavbar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import Loading from '@/components/Loading/Loading.vue'
import { detectApi } from '@/common/api/index.js'

const route = useRoute()
const taskId = route.query.taskId

const loading = ref(false)
const artifact = ref(null)

onMounted(() => {
  loadResult()
})

const loadResult = async () => {
  if (!taskId) return

  loading.value = true

  try {
    const res = await detectApi.getTaskResult(taskId)
    artifact.value = res.data
  } catch (error) {
    console.error('加载结果失败:', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

const previewImage = () => {
  if (artifact.value?.imageUrl) {
    uni.previewImage({
      urls: [artifact.value.imageUrl]
    })
  }
}

const handleAskAI = () => {
  if (!artifact.value) return

  uni.navigateTo({
    url: `/pages/chat/chat?artifactId=${artifact.value.id}&artifactName=${encodeURIComponent(artifact.value.name)}`
  })
}

const handleShare = () => {
  uni.showShareMenu({
    withShareTicket: true
  })
}
</script>

<style lang="scss" scoped>
.result-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);
  padding-top: calc(var(--status-bar-height) + 88rpx);

  .result-content {
    padding: 32rpx;

    .result-card {
      background: #FFF8DC;
      border-radius: 24rpx;
      overflow: hidden;
      box-shadow: 0 8rpx 24rpx rgba(139, 69, 19, 0.1);
      margin-bottom: 32rpx;
      border: 2rpx solid #D2B48C;
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        top: 12rpx;
        left: 12rpx;
        right: 12rpx;
        bottom: 12rpx;
        border: 1rpx solid rgba(218, 165, 32, 0.3);
        border-radius: 20rpx;
        pointer-events: none;
      }

      .artifact-image {
        position: relative;
        width: 100%;
        height: 500rpx;

        .image {
          width: 100%;
          height: 100%;
        }

        .confidence-badge {
          position: absolute;
          top: 24rpx;
          right: 24rpx;
          background: rgba(139, 69, 19, 0.85);
          border-radius: 24rpx;
          padding: 12rpx 24rpx;
          border: 2rpx solid #DAA520;

          .confidence-text {
            font-size: 24rpx;
            color: #FFF8DC;
          }
        }
      }

      .artifact-info {
        padding: 32rpx;

        .info-header {
          margin-bottom: 32rpx;

          .artifact-name {
            display: block;
            font-size: 40rpx;
            color: #5D4037;
            font-weight: 600;
            margin-bottom: 16rpx;
            font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
          }

          .artifact-tags {
            display: flex;
            gap: 12rpx;
            flex-wrap: wrap;

            .tag {
              background: rgba(139, 69, 19, 0.1);
              color: #8B4513;
              font-size: 24rpx;
              padding: 8rpx 16rpx;
              border-radius: 8rpx;
              border: 1rpx solid rgba(139, 69, 19, 0.2);
            }
          }
        }

        .info-section {
          margin-bottom: 32rpx;

          &:last-child {
            margin-bottom: 0;
          }

          .section-title {
            display: block;
            font-size: 28rpx;
            color: #5D4037;
            font-weight: 600;
            margin-bottom: 16rpx;
            font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
          }

          .info-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 24rpx;

            .info-item {
              background: #FAF0E6;
              border-radius: 12rpx;
              padding: 24rpx;
              border: 2rpx solid #D2B48C;

              .info-label {
                display: block;
                font-size: 24rpx;
                color: #8B7355;
                margin-bottom: 8rpx;
              }

              .info-value {
                display: block;
                font-size: 28rpx;
                color: #5D4037;
                font-weight: 500;
              }
            }
          }

          .description-text {
            display: block;
            font-size: 28rpx;
            color: #5D4037;
            line-height: 1.8;
          }
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 24rpx;

      .action-btn {
        flex: 1;
        height: 96rpx;
        border-radius: 12rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12rpx;
        font-size: 28rpx;
        font-weight: 600;
        border: none;

        .btn-icon {
          font-size: 32rpx;
        }

        &.primary {
          background: linear-gradient(135deg, #8B4513 0%, #65320F 100%);
          color: #FFF8DC;
          border: 2rpx solid #8B4513;
        }

        &.secondary {
          background: #FFF8DC;
          color: #8B4513;
          border: 2rpx solid #8B4513;
        }
      }
    }
  }
}
</style>