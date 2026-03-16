<template>
  <view class="index-page">
    <view class="page-header">
      <view class="header-content">
        <view class="user-info" @click="goToProfile">
          <image class="avatar" :src="userInfo?.avatar || '/static/images/default-avatar.png'" mode="aspectFill"></image>
          <view class="user-text">
            <text class="greeting">你好，{{ userInfo?.username || '游客' }}</text>
            <text class="role-name">{{ getRoleName(userInfo?.roles?.[0]) }}</text>
          </view>
        </view>
        <view class="header-actions">
          <view class="action-btn" @click="goToFeedback">
            <text class="action-icon">💬</text>
          </view>
        </view>
      </view>
    </view>

    <view class="page-content">
      <view class="banner-section">
        <swiper class="banner-swiper" :indicator-dots="true" :autoplay="true" :interval="3000" :duration="500">
          <swiper-item v-for="(item, index) in banners" :key="index">
            <image class="banner-image" :src="item.image" mode="aspectFill"></image>
            <view class="banner-text">
              <text class="banner-title">{{ item.title }}</text>
              <text class="banner-desc">{{ item.desc }}</text>
            </view>
          </swiper-item>
        </swiper>
      </view>

      <view class="quick-actions">
        <view class="action-card" @click="goToArtifacts">
          <view class="action-icon-wrapper" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <text class="action-icon">🏺</text>
          </view>
          <text class="action-label">文物库</text>
        </view>
        <view class="action-card" @click="goToChat">
          <view class="action-icon-wrapper" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <text class="action-icon">🤖</text>
          </view>
          <text class="action-label">AI问答</text>
        </view>
        <view class="action-card" @click="goToHistory">
          <view class="action-icon-wrapper" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <text class="action-icon">📜</text>
          </view>
          <text class="action-label">识别记录</text>
        </view>
        <view class="action-card" @click="goToAbout">
          <view class="action-icon-wrapper" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
            <text class="action-icon">ℹ️</text>
          </view>
          <text class="action-label">关于我们</text>
        </view>
      </view>

      <view class="scan-section">
        <view class="scan-header">
          <text class="section-title">文物识别</text>
          <text class="section-desc">拍照或上传图片，AI智能识别文物</text>
        </view>
        <ScanButton text="点击拍照识别" @scan="handleScan" />
      </view>

      <view class="recent-section" v-if="recentArtifacts.length > 0">
        <view class="section-header">
          <text class="section-title">最近识别</text>
          <text class="more-link" @click="goToHistory">查看更多</text>
        </view>
        <scroll-view class="recent-scroll" scroll-x="true">
          <view class="recent-list">
            <view
              class="recent-item"
              v-for="(item, index) in recentArtifacts"
              :key="index"
              @click="goToDetail(item.id)"
            >
              <image class="recent-image" :src="item.image" mode="aspectFill"></image>
              <text class="recent-name">{{ item.name }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import ScanButton from '@/components/ScanButton/ScanButton.vue'
import { chooseImage, processImageForUpload } from '@/common/utils/image.js'
import { detectApi } from '@/common/api/index.js'
import { pollTaskStatus } from '@/common/utils/async.js'
import { getRoleName } from '@/common/utils/index.js'

const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)

const banners = ref([
  {
    image: '/static/images/banner1.png',
    title: '探索中华文明',
    desc: 'AI智能识别，让文物活起来'
  },
  {
    image: '/static/images/banner2.png',
    title: '传承历史记忆',
    desc: '发现身边的文化瑰宝'
  }
])

const recentArtifacts = ref([])

onMounted(() => {
  loadRecentArtifacts()
})

const loadRecentArtifacts = () => {
  const history = uni.getStorageSync('detectHistory') || []
  recentArtifacts.value = history.slice(0, 5)
}

const handleScan = async () => {
  try {
    const filePaths = await chooseImage(1)
    const filePath = filePaths[0]

    uni.showLoading({ title: '处理图片中...' })

    const processed = await processImageForUpload(filePath)

    uni.showLoading({ title: '上传中...' })

    const taskRes = await detectApi.createTask({
      imageBase64: processed.base64
    })

    const taskId = taskRes.data.taskId

    uni.showLoading({ title: '识别中...' })

    const result = await pollTaskStatus(
      taskId,
      detectApi.getTaskStatus,
      detectApi.getTaskResult
    )

    uni.hideLoading()

    saveToHistory(result.data)

    uni.navigateTo({
      url: `/pages/result/result?taskId=${taskId}`
    })
  } catch (error) {
    uni.hideLoading()
    console.error('识别失败:', error)
    uni.showToast({
      title: error.message || '识别失败，请重试',
      icon: 'none'
    })
  }
}

const saveToHistory = (artifact) => {
  const history = uni.getStorageSync('detectHistory') || []
  history.unshift(artifact)
  uni.setStorageSync('detectHistory', history.slice(0, 20))
  loadRecentArtifacts()
}

const goToProfile = () => {
  uni.switchTab({
    url: '/pages/profile/profile'
  })
}

const goToFeedback = () => {
  uni.navigateTo({
    url: '/pages/feedback/feedback'
  })
}

const goToArtifacts = () => {
  uni.switchTab({
    url: '/pages/artifacts/artifacts'
  })
}

const goToChat = () => {
  uni.switchTab({
    url: '/pages/chat/chat'
  })
}

const goToHistory = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

const goToAbout = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

const goToDetail = (id) => {
  uni.navigateTo({
    url: `/pages/artifact-detail/artifact-detail?id=${id}`
  })
}
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .page-header {
    background: linear-gradient(135deg, #8B4513 0%, #65320F 100%);
    padding: 32rpx;
    padding-top: calc(var(--status-bar-height) + 32rpx);
    position: relative;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 4rpx;
      background: linear-gradient(90deg, transparent, #DAA520, transparent);
    }

    .header-content {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .user-info {
        display: flex;
        align-items: center;
        gap: 24rpx;

        .avatar {
          width: 96rpx;
          height: 96rpx;
          border-radius: 50%;
          border: 4rpx solid rgba(218, 165, 32, 0.5);
          box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
        }

        .user-text {
          display: flex;
          flex-direction: column;
          gap: 8rpx;

          .greeting {
            font-size: 32rpx;
            color: #FFF8DC;
            font-weight: 600;
            font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
          }

          .role-name {
            font-size: 24rpx;
            color: rgba(255, 248, 220, 0.8);
          }
        }
      }

      .header-actions {
        display: flex;
        gap: 16rpx;

        .action-btn {
          width: 72rpx;
          height: 72rpx;
          background: rgba(218, 165, 32, 0.2);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2rpx solid rgba(218, 165, 32, 0.3);

          .action-icon {
            font-size: 36rpx;
          }
        }
      }
    }
  }

  .page-content {
    padding: 32rpx;

    .banner-section {
      margin-bottom: 32rpx;
      border-radius: 24rpx;
      overflow: hidden;
      box-shadow: 0 8rpx 24rpx rgba(139, 69, 19, 0.15);
      border: 2rpx solid #D2B48C;

      .banner-swiper {
        width: 100%;
        height: 360rpx;

        .banner-image {
          width: 100%;
          height: 100%;
        }

        .banner-text {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          background: linear-gradient(to top, rgba(139, 69, 19, 0.85), transparent);
          padding: 40rpx 32rpx 32rpx;
          border-top: 2rpx solid rgba(218, 165, 32, 0.5);

          .banner-title {
            display: block;
            font-size: 36rpx;
            color: #FFF8DC;
            font-weight: 600;
            margin-bottom: 8rpx;
            font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
            text-shadow: 2rpx 2rpx 4rpx rgba(0, 0, 0, 0.3);
          }

          .banner-desc {
            display: block;
            font-size: 24rpx;
            color: rgba(255, 248, 220, 0.9);
          }
        }
      }
    }

    .quick-actions {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 24rpx;
      margin-bottom: 48rpx;

      .action-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16rpx;

        .action-icon-wrapper {
          width: 112rpx;
          height: 112rpx;
          border-radius: 24rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 8rpx 16rpx rgba(139, 69, 19, 0.15);
          border: 2rpx solid rgba(210, 180, 140, 0.3);

          .action-icon {
            font-size: 48rpx;
          }
        }

        .action-label {
          font-size: 24rpx;
          color: #5D4037;
          font-weight: 500;
        }
      }
    }

    .scan-section {
      background: #FFF8DC;
      border-radius: 24rpx;
      padding: 48rpx;
      margin-bottom: 48rpx;
      box-shadow: 0 8rpx 24rpx rgba(139, 69, 19, 0.1);
      display: flex;
      flex-direction: column;
      align-items: center;
      border: 2rpx solid #D2B48C;
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        top: 16rpx;
        left: 16rpx;
        right: 16rpx;
        bottom: 16rpx;
        border: 1rpx solid rgba(218, 165, 32, 0.3);
        border-radius: 16rpx;
        pointer-events: none;
      }

      .scan-header {
        text-align: center;
        margin-bottom: 40rpx;

        .section-title {
          display: block;
          font-size: 36rpx;
          color: #5D4037;
          font-weight: 600;
          margin-bottom: 12rpx;
          font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
        }

        .section-desc {
          display: block;
          font-size: 24rpx;
          color: #8B7355;
        }
      }
    }

    .recent-section {
      .section-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 24rpx;

        .section-title {
          font-size: 32rpx;
          color: #5D4037;
          font-weight: 600;
          font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
        }

        .more-link {
          font-size: 24rpx;
          color: #8B4513;
        }
      }

      .recent-scroll {
        white-space: nowrap;

        .recent-list {
          display: flex;
          gap: 20rpx;

          .recent-item {
            display: inline-block;
            width: 200rpx;
            flex-shrink: 0;

            .recent-image {
              width: 200rpx;
              height: 200rpx;
              border-radius: 16rpx;
              margin-bottom: 12rpx;
              border: 2rpx solid #D2B48C;
              box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
            }

            .recent-name {
              display: block;
              font-size: 24rpx;
              color: #5D4037;
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
</style>