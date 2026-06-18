<template>
  <view class="result-page">
    <CustomNavbar title="识别结果" :show-back="true" />

    <view class="result-content">
      <view class="result-card" v-if="artifact">
        <view class="artifact-image">
          <image class="image" :src="artifact.imageUrl" mode="aspectFill" @click="previewImage"></image>
          <view class="confidence-badge" v-if="artifact.confidence !== null">
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

        </view>
      </view>

      <EmptyState v-else :image="emptyResultImage" text="暂无识别结果" />

      <view class="action-buttons" v-if="artifact">
        <button class="action-btn secondary" @click="handleAskAI">
          <image class="btn-icon" :src="chatIcon" mode="aspectFit"></image>
          <text class="btn-text">向AI提问</text>
        </button>
        <button class="action-btn secondary" @click="toggleFavorite">
          <image class="btn-icon" :src="favoriteIcon" mode="aspectFit"></image>
          <text class="btn-text">{{ isFavorited ? '已收藏' : '收藏' }}</text>
        </button>
        <button class="action-btn primary" open-type="share" @click="handleShare">
          <image class="btn-icon" :src="shareIcon" mode="aspectFit"></image>
          <text class="btn-text">分享</text>
        </button>
      </view>
    </view>

    <Loading :visible="loading" text="加载中..." />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad, onShow, onShareAppMessage, onShareTimeline } from '@dcloudio/uni-app'
import CustomNavbar from '@/components/CustomNavbar/CustomNavbar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import Loading from '@/components/Loading/Loading.vue'
import { artifactsApi, detectApi } from '@/common/api/index.js'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'
import { loadArtifactDescription, normalizeOssUrl } from '@/common/utils/oss.js'

const taskId = ref('')
const emptyResultImage = resolveAssetPath('/static/images/empty-result.png')
const chatIcon = resolveAssetPath('/static/icons/chat.svg')
const favoriteIcon = resolveAssetPath('/static/icons/favorite.svg')
const shareIcon = resolveAssetPath('/static/icons/share.svg')
const PENDING_CHAT_ARTIFACT_KEY = 'pendingChatArtifactContext'

onLoad((options = {}) => {
  taskId.value = options.taskId || ''
})

const loading = ref(false)
const artifact = ref(null)
const isFavorited = ref(false)

onMounted(() => {
  loadResult()
})

onShow(() => {
  syncFavoriteState()
})

const getFavoriteIds = () => {
  const favorites = uni.getStorageSync('favorites') || []
  return favorites
    .map((item) => {
      if (typeof item === 'number' || typeof item === 'string') {
        return Number(item)
      }
      return Number(item?.id || item?.artifactId)
    })
    .filter((id) => Number.isFinite(id) && id > 0)
}

const syncFavoriteState = () => {
  const currentId = Number(artifact.value?.id)
  if (!Number.isFinite(currentId) || currentId <= 0) {
    isFavorited.value = false
    return
  }

  isFavorited.value = getFavoriteIds().includes(currentId)
}

const loadResult = async () => {
  if (!taskId.value) return

  loading.value = true

  try {
    const detectRes = await detectApi.getTaskResult(taskId.value)
    const detectData = detectRes.data
    const firstMatch = detectData?.detectedArtifacts?.[0] || null

    let detail = null
    if (firstMatch?.artifactId) {
      const detailRes = await artifactsApi.getDetail(firstMatch.artifactId)
      detail = detailRes.data
    }

    const detailDescription = await loadArtifactDescription(detail?.description)

    artifact.value = {
      id: detail?.id || firstMatch?.artifactId || detectData?.taskId,
      name: detail?.name || firstMatch?.label || '识别结果',
      imageUrl: normalizeOssUrl(detail?.imageUrl || detectData?.imageUrl, emptyResultImage),
      confidence: typeof firstMatch?.confidence === 'number'
        ? Math.round(firstMatch.confidence * 100)
        : null,
      tags: detail?.tags || [],
      period: detail?.era,
      material: detail?.category,
      size: null,
      location: detail?.location,
      description: detailDescription
    }
    syncFavoriteState()
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
  if (!artifact.value?.id) {
    uni.showToast({
      title: '缺少文物信息',
      icon: 'none'
    })
    return
  }

  uni.setStorageSync(PENDING_CHAT_ARTIFACT_KEY, {
    artifactId: String(artifact.value.id),
    artifactName: artifact.value.name || ''
  })

  uni.switchTab({
    url: '/pages/chat/chat'
  })
}

const handleShare = () => {
  // #ifdef MP-WEIXIN
  uni.showShareMenu({
    menus: ['shareAppMessage', 'shareTimeline']
  })
  uni.showToast({
    title: '请选择要分享的渠道',
    icon: 'none'
  })
  // #endif

  // #ifndef MP-WEIXIN
  uni.showToast({
    title: '请使用微信小程序进行分享',
    icon: 'none'
  })
  // #endif
}

onShareAppMessage(() => {
  const title = artifact.value?.name
    ? `我在稽古云语识别到了文物：${artifact.value.name}`
    : '我在稽古云语完成了一次文物识别'
  const imageUrl = artifact.value?.imageUrl || ''
  const id = artifact.value?.id

  return {
    title,
    path: id ? `/pages/artifact-detail/artifact-detail?id=${id}` : '/pages/index/index',
    imageUrl
  }
})

onShareTimeline(() => {
  const title = artifact.value?.name
    ? `稽古云语识别结果：${artifact.value.name}`
    : '稽古云语识别结果分享'
  const imageUrl = artifact.value?.imageUrl || ''
  const id = artifact.value?.id

  return {
    title,
    query: id ? `id=${id}` : '',
    imageUrl
  }
})

const toggleFavorite = () => {
  if (!artifact.value?.id) {
    uni.showToast({
      title: '缺少文物信息',
      icon: 'none'
    })
    return
  }

  const currentId = Number(artifact.value.id)
  const favorites = uni.getStorageSync('favorites') || []
  const ids = favorites
    .map((item) => {
      if (typeof item === 'number' || typeof item === 'string') {
        return Number(item)
      }
      return Number(item?.id || item?.artifactId)
    })
    .filter((id) => Number.isFinite(id) && id > 0)

  const hasFavorite = ids.includes(currentId)
  const nextFavorites = hasFavorite
    ? favorites.filter((item) => {
        if (typeof item === 'number' || typeof item === 'string') {
          return Number(item) !== currentId
        }
        return Number(item?.id || item?.artifactId) !== currentId
      })
    : [
        ...favorites,
        {
          id: currentId,
          artifactId: currentId,
          name: artifact.value.name || '',
          imageUrl: artifact.value.imageUrl || '',
          era: artifact.value.period || '',
          category: artifact.value.material || ''
        }
      ]

  uni.setStorageSync('favorites', nextFavorites)
  syncFavoriteState()
  uni.showToast({
    title: hasFavorite ? '已取消收藏' : '收藏成功',
    icon: 'success'
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
            white-space: pre-wrap;
            word-break: break-all;
            overflow-wrap: anywhere;
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
          width: 30rpx;
          height: 30rpx;
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