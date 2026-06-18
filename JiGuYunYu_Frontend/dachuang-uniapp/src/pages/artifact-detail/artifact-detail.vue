<template>
  <view class="artifact-detail-page">
    <CustomNavbar title="文物详情" :show-back="true" />

    <scroll-view class="detail-content" scroll-y="true">
      <view class="detail-card" v-if="artifact">
        <view class="image-section">
          <swiper class="image-swiper" :indicator-dots="true" :autoplay="false">
            <swiper-item v-for="(image, index) in artifactImages" :key="index">
              <image class="detail-image" :src="image" mode="aspectFill" @click="previewImage(index)"></image>
            </swiper-item>
          </swiper>
        </view>

        <view class="info-section">
          <view class="artifact-header">
            <text class="artifact-name">{{ artifact.name }}</text>
            <view class="status-badge" :style="{ backgroundColor: getStatusColor(artifact.status) }">
              <text class="status-text">{{ getStatusText(artifact.status) }}</text>
            </view>
          </view>

          <view class="artifact-tags" v-if="artifact.tags && artifact.tags.length > 0">
            <text class="tag" v-for="(tag, index) in artifact.tags" :key="index">{{ tag }}</text>
          </view>

          <view class="basic-info">
            <view class="info-row">
              <text class="info-label">年代</text>
              <text class="info-value">{{ artifact.era || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">材质</text>
              <text class="info-value">{{ artifact.category || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">出土地</text>
              <text class="info-value">{{ artifact.location || '未知' }}</text>
            </view>
          </view>
        </view>

        <view class="description-section" v-if="artifact.description">
          <view class="section-title">文物简介</view>
          <text class="description-text">{{ artifact.description }}</text>
        </view>

      </view>

      <EmptyState v-else :image="emptyImage" text="暂无数据" />
    </scroll-view>

    <view class="action-bar" v-if="artifact">
      <button class="action-btn secondary" @click="handleAskAI">
        <image class="btn-icon" :src="chatIcon" mode="aspectFit"></image>
        <text class="btn-text">AI问答</text>
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

    <Loading :visible="loading" text="加载中..." />
  </view>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { onLoad, onShow, onShareAppMessage, onShareTimeline } from '@dcloudio/uni-app'
import CustomNavbar from '@/components/CustomNavbar/CustomNavbar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import Loading from '@/components/Loading/Loading.vue'
import { artifactsApi } from '@/common/api/index.js'
import { getStatusText, getStatusColor } from '@/common/utils/index.js'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'
import { loadArtifactDescription, normalizeOssUrl } from '@/common/utils/oss.js'

const artifactId = ref('')
const emptyImage = resolveAssetPath('/static/images/empty.png')
const chatIcon = resolveAssetPath('/static/icons/chat.svg')
const favoriteIcon = resolveAssetPath('/static/icons/favorite.svg')
const shareIcon = resolveAssetPath('/static/icons/share.svg')
const PENDING_CHAT_ARTIFACT_KEY = 'pendingChatArtifactContext'

onLoad((options = {}) => {
  artifactId.value = options.id || ''
})

const loading = ref(false)
const artifact = ref(null)
const isFavorited = ref(false)
const artifactImages = computed(() => {
  if (!artifact.value) return []
  const images = [artifact.value.imageUrl, artifact.value.thumbnailUrl].filter(Boolean)
  return [...new Set(images)]
})

onMounted(() => {
  loadDetail()
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
  const currentId = Number(artifact.value?.id || artifactId.value)
  if (!Number.isFinite(currentId) || currentId <= 0) {
    isFavorited.value = false
    return
  }

  isFavorited.value = getFavoriteIds().includes(currentId)
}

const loadDetail = async () => {
  if (!artifactId.value) return

  loading.value = true

  try {
    const res = await artifactsApi.getDetail(artifactId.value)
    const raw = res.data || {}
    const description = await loadArtifactDescription(raw.description)

    artifact.value = {
      ...raw,
      imageUrl: normalizeOssUrl(raw.imageUrl, ''),
      thumbnailUrl: normalizeOssUrl(raw.thumbnailUrl, ''),
      description
    }
    syncFavoriteState()
  } catch (error) {
    console.error('加载详情失败:', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

const previewImage = (index) => {
  if (artifactImages.value.length > 0) {
    uni.previewImage({
      urls: artifactImages.value,
      current: index
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
    ? `我在稽古云语发现了文物：${artifact.value.name}`
    : '我在稽古云语发现了有趣文物'
  const imageUrl = artifact.value?.imageUrl || artifact.value?.thumbnailUrl || ''
  const id = artifact.value?.id || artifactId.value

  return {
    title,
    path: id ? `/pages/artifact-detail/artifact-detail?id=${id}` : '/pages/artifacts/artifacts',
    imageUrl
  }
})

onShareTimeline(() => {
  const title = artifact.value?.name
    ? `稽古云语文物分享：${artifact.value.name}`
    : '稽古云语文物分享'
  const imageUrl = artifact.value?.imageUrl || artifact.value?.thumbnailUrl || ''
  const id = artifact.value?.id || artifactId.value

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
          thumbnailUrl: artifact.value.thumbnailUrl || '',
          era: artifact.value.era || '',
          category: artifact.value.category || ''
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
.artifact-detail-page {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 120rpx;

  .detail-content {
    padding-top: calc(var(--status-bar-height) + 88rpx);

    .detail-card {
      .image-section {
        background: #fff;
        padding-bottom: 32rpx;

        .image-swiper {
          width: 100%;
          height: 600rpx;

          .detail-image {
            width: 100%;
            height: 100%;
          }
        }
      }

      .info-section {
        background: #fff;
        padding: 32rpx;
        margin-top: 16rpx;

        .artifact-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 24rpx;

          .artifact-name {
            flex: 1;
            font-size: 40rpx;
            color: #333;
            font-weight: 600;
            margin-right: 16rpx;
          }

          .status-badge {
            padding: 8rpx 16rpx;
            border-radius: 8rpx;

            .status-text {
              font-size: 24rpx;
              color: #fff;
            }
          }
        }

        .artifact-tags {
          display: flex;
          gap: 12rpx;
          flex-wrap: wrap;
          margin-bottom: 32rpx;

          .tag {
            background: #F0F0F0;
            color: #666;
            font-size: 24rpx;
            padding: 8rpx 16rpx;
            border-radius: 8rpx;
          }
        }

        .basic-info {
          .info-row {
            display: flex;
            justify-content: space-between;
            padding: 20rpx 0;
            border-bottom: 1rpx solid #F0F0F0;

            &:last-child {
              border-bottom: none;
            }

            .info-label {
              font-size: 28rpx;
              color: #999;
            }

            .info-value {
              font-size: 28rpx;
              color: #333;
              text-align: right;
              flex: 1;
              margin-left: 32rpx;
            }
          }
        }
      }

      .description-section,
      .history-section,
      .craft-section {
        background: #fff;
        padding: 32rpx;
        margin-top: 16rpx;

        .section-title {
          font-size: 32rpx;
          color: #333;
          font-weight: 600;
          margin-bottom: 20rpx;
        }

        .description-text {
          display: block;
          white-space: pre-wrap;
          word-break: break-all;
          overflow-wrap: anywhere;
          font-size: 28rpx;
          color: #666;
          line-height: 1.8;
        }
      }

      .related-section {
        background: #fff;
        padding: 32rpx;
        margin-top: 16rpx;

        .section-title {
          font-size: 32rpx;
          color: #333;
          font-weight: 600;
          margin-bottom: 24rpx;
        }

        .related-scroll {
          white-space: nowrap;

          .related-list {
            display: flex;
            gap: 20rpx;

            .related-item {
              display: inline-block;
              width: 240rpx;
              flex-shrink: 0;

              .related-image {
                width: 240rpx;
                height: 240rpx;
                border-radius: 12rpx;
                margin-bottom: 12rpx;
              }

              .related-name {
                display: block;
                font-size: 24rpx;
                color: #333;
                text-align: center;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              }
            }
          }
        }
      }
    }
  }

  .action-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    padding: 24rpx 32rpx;
    padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
    box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
    display: flex;
    gap: 24rpx;

    .action-btn {
      flex: 1;
      height: 88rpx;
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
        background: linear-gradient(135deg, #C8102E 0%, #8B0A1F 100%);
        color: #fff;
      }

      &.secondary {
        background: #fff;
        color: #C8102E;
        border: 2rpx solid #C8102E;
      }
    }
  }
}
</style>