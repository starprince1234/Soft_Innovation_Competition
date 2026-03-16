<template>
  <view class="artifact-detail-page">
    <CustomNavbar title="文物详情" :show-back="true" />

    <scroll-view class="detail-content" scroll-y="true">
      <view class="detail-card" v-if="artifact">
        <view class="image-section">
          <swiper class="image-swiper" :indicator-dots="true" :autoplay="false">
            <swiper-item v-for="(image, index) in artifact.images" :key="index">
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
              <text class="info-value">{{ artifact.period || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">材质</text>
              <text class="info-value">{{ artifact.material || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">尺寸</text>
              <text class="info-value">{{ artifact.size || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">重量</text>
              <text class="info-value">{{ artifact.weight || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">出土地</text>
              <text class="info-value">{{ artifact.location || '未知' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">收藏单位</text>
              <text class="info-value">{{ artifact.museum || '未知' }}</text>
            </view>
          </view>
        </view>

        <view class="description-section" v-if="artifact.description">
          <view class="section-title">文物简介</view>
          <text class="description-text">{{ artifact.description }}</text>
        </view>

        <view class="history-section" v-if="artifact.historicalSignificance">
          <view class="section-title">历史意义</view>
          <text class="description-text">{{ artifact.historicalSignificance }}</text>
        </view>

        <view class="craft-section" v-if="artifact.craftsmanship">
          <view class="section-title">工艺特点</view>
          <text class="description-text">{{ artifact.craftsmanship }}</text>
        </view>

        <view class="related-section" v-if="relatedArtifacts.length > 0">
          <view class="section-title">相关文物</view>
          <scroll-view class="related-scroll" scroll-x="true">
            <view class="related-list">
              <view
                class="related-item"
                v-for="(item, index) in relatedArtifacts"
                :key="index"
                @click="goToDetail(item.id)"
              >
                <image class="related-image" :src="item.imageUrl" mode="aspectFill"></image>
                <text class="related-name">{{ item.name }}</text>
              </view>
            </view>
          </scroll-view>
        </view>
      </view>

      <EmptyState v-else image="/static/images/empty.png" text="暂无数据" />
    </scroll-view>

    <view class="action-bar" v-if="artifact">
      <button class="action-btn secondary" @click="handleAskAI">
        <text class="btn-icon">🤖</text>
        <text class="btn-text">AI问答</text>
      </button>
      <button class="action-btn primary" @click="handleShare">
        <text class="btn-icon">📤</text>
        <text class="btn-text">分享</text>
      </button>
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
import { artifactsApi } from '@/common/api/index.js'
import { getStatusText, getStatusColor } from '@/common/utils/index.js'

const route = useRoute()
const artifactId = route.query.id

const loading = ref(false)
const artifact = ref(null)
const relatedArtifacts = ref([])

onMounted(() => {
  loadDetail()
})

const loadDetail = async () => {
  if (!artifactId) return

  loading.value = true

  try {
    const res = await artifactsApi.getDetail(artifactId)
    artifact.value = res.data

    if (res.data.relatedArtifacts) {
      relatedArtifacts.value = res.data.relatedArtifacts
    }
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
  if (artifact.value?.images) {
    uni.previewImage({
      urls: artifact.value.images,
      current: index
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

const goToDetail = (id) => {
  uni.redirectTo({
    url: `/pages/artifact-detail/artifact-detail?id=${id}`
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
        font-size: 32rpx;
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