<template>
  <view class="profile-page">
    <view class="profile-header">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="user-info" @click="handleEditProfile">
          <image class="avatar" :src="userInfo?.avatar || defaultAvatarImage" mode="aspectFill"></image>
          <view class="user-text">
            <text class="username">{{ userInfo?.username || '未设置' }}</text>
            <text class="user-role">{{ getRoleName(userInfo?.role) }}</text>
          </view>
          <image class="edit-icon" :src="editIcon" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <view class="profile-content">
      <view class="stats-section">
        <view class="stat-item">
          <text class="stat-value">{{ stats.detectCount }}</text>
          <text class="stat-label">识别次数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ stats.chatCount }}</text>
          <text class="stat-label">对话次数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ stats.favoriteCount }}</text>
          <text class="stat-label">收藏数量</text>
        </view>
      </view>

      <view class="menu-section">
        <view class="menu-item" @click="goToHistory">
          <view class="menu-left">
            <image class="menu-icon" :src="historyIcon" mode="aspectFit"></image>
            <text class="menu-label">识别记录</text>
          </view>
          <image class="menu-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="menu-item" @click="goToFavorites">
          <view class="menu-left">
            <image class="menu-icon" :src="favoriteIcon" mode="aspectFit"></image>
            <text class="menu-label">我的收藏</text>
          </view>
          <image class="menu-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="menu-item" @click="goToFeedbackList">
          <view class="menu-left">
            <image class="menu-icon" :src="feedbackIcon" mode="aspectFit"></image>
            <text class="menu-label">我的反馈</text>
          </view>
          <image class="menu-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
      </view>

      <view class="menu-section">
        <view class="menu-item" @click="goToSettings">
          <view class="menu-left">
            <image class="menu-icon" :src="settingsIcon" mode="aspectFit"></image>
            <text class="menu-label">设置</text>
          </view>
          <image class="menu-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="menu-item" @click="goToAbout">
          <view class="menu-left">
            <image class="menu-icon" :src="aboutIcon" mode="aspectFit"></image>
            <text class="menu-label">关于我们</text>
          </view>
          <image class="menu-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="menu-item" @click="goToHelp">
          <view class="menu-left">
            <image class="menu-icon" :src="helpIcon" mode="aspectFit"></image>
            <text class="menu-label">帮助中心</text>
          </view>
          <image class="menu-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
      </view>

      <view class="menu-section">
        <view class="menu-item" @click="handleClearCache">
          <view class="menu-left">
            <image class="menu-icon" :src="trashIcon" mode="aspectFit"></image>
            <text class="menu-label">清除缓存</text>
          </view>
          <text class="menu-desc">{{ cacheSize }}</text>
        </view>
      </view>

      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { getRoleName } from '@/common/utils/index.js'
import { authApi } from '@/common/api/index.js'
import {
  clearAssetCacheFiles,
  getAssetCacheSizeBytes,
  resolveAssetPath
} from '@/common/utils/asset-cache.js'
import { onShow } from '@dcloudio/uni-app'

const userStore = useUserStore()
const defaultAvatarImage = resolveAssetPath('/static/images/default-avatar.png')
const editIcon = resolveAssetPath('/static/icons/edit.svg')
const historyIcon = resolveAssetPath('/static/icons/history.svg')
const favoriteIcon = resolveAssetPath('/static/icons/favorite.svg')
const feedbackIcon = resolveAssetPath('/static/icons/feedback.svg')
const settingsIcon = resolveAssetPath('/static/icons/settings.svg')
const aboutIcon = resolveAssetPath('/static/icons/about.svg')
const helpIcon = resolveAssetPath('/static/icons/help.svg')
const trashIcon = resolveAssetPath('/static/icons/trash.svg')
const chevronRightIcon = resolveAssetPath('/static/icons/chevron-right.svg')

const userInfo = computed(() => userStore.userInfo)

const stats = ref({
  detectCount: 0,
  chatCount: 0,
  favoriteCount: 0
})

const cacheSize = ref('0 B')

onMounted(() => {
  loadStats()
  loadCacheSize()
})

onShow(() => {
  loadStats()
  loadCacheSize()
})

const loadStats = () => {
  const history = uni.getStorageSync('detectHistory') || []
  const chatHistory = uni.getStorageSync('chatHistory') || []
  const favorites = uni.getStorageSync('favorites') || []

  stats.value = {
    detectCount: history.length,
    chatCount: chatHistory.length,
    favoriteCount: favorites.length
  }
}

const formatCacheSize = (bytes) => {
  const size = Number(bytes) || 0

  if (size < 1024) {
    return `${size} B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(2)} KB`
  }

  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

const loadCacheSize = async () => {
  try {
    const storageInfo = uni.getStorageInfoSync()
    const storageBytes = (Number(storageInfo.currentSize) || 0) * 1024
    const assetCacheBytes = await getAssetCacheSizeBytes()

    cacheSize.value = formatCacheSize(storageBytes + assetCacheBytes)
  } catch (error) {
    console.error('获取缓存大小失败:', error)
  }
}

const handleEditProfile = () => {
  uni.navigateTo({
    url: '/pages/edit-profile/edit-profile'
  })
}

const goToHistory = () => {
  uni.navigateTo({
    url: '/pages/history/history'
  })
}

const goToFavorites = () => {
  uni.navigateTo({
    url: '/pages/favorites/favorites'
  })
}

const goToFeedbackList = () => {
  uni.navigateTo({
    url: '/pages/feedback/feedback'
  })
}

const goToSettings = () => {
  uni.navigateTo({
    url: '/pages/settings/settings'
  })
}

const goToAbout = () => {
  uni.navigateTo({
    url: '/pages/about/about'
  })
}

const goToHelp = () => {
  uni.navigateTo({
    url: '/pages/help/help'
  })
}

const handleClearCache = () => {
  uni.showModal({
    title: '提示',
    content: '确定要清除缓存吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await clearAssetCacheFiles()
          uni.clearStorageSync()
          cacheSize.value = '0 B'
          stats.value = {
            detectCount: 0,
            chatCount: 0,
            favoriteCount: 0
          }
          uni.showToast({
            title: '清除成功',
            icon: 'success'
          })
        } catch (error) {
          console.error('清除缓存失败:', error)
          uni.showToast({
            title: '清除失败',
            icon: 'none'
          })
        }
      }
    }
  })
}

const handleLogout = async () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '退出中...' })
          await authApi.logout()
          uni.hideLoading()
          userStore.logout()
        } catch (error) {
          uni.hideLoading()
          console.error('登出请求失败:', error)
          userStore.logout()
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .profile-header {
    position: relative;
    padding-bottom: 40rpx;

    .header-bg {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 320rpx;
      background: linear-gradient(135deg, #8B4513 0%, #65320F 100%);
      border-radius: 0 0 48rpx 48rpx;
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
    }

    .header-content {
      position: relative;
      padding: calc(var(--status-bar-height) + 32rpx) 32rpx 0;

      .user-info {
        display: flex;
        align-items: center;
        gap: 24rpx;
        padding: 32rpx;
        background: #FFF8DC;
        border-radius: 24rpx;
        box-shadow: 0 8rpx 24rpx rgba(139, 69, 19, 0.15);
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

        .avatar {
          width: 120rpx;
          height: 120rpx;
          border-radius: 50%;
          border: 4rpx solid #DAA520;
          box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
        }

        .user-text {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 8rpx;

          .username {
            font-size: 36rpx;
            color: #5D4037;
            font-weight: 600;
            font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
          }

          .user-role {
            font-size: 24rpx;
            color: #8B7355;
          }
        }

        .edit-icon {
          width: 36rpx;
          height: 36rpx;
        }
      }
    }
  }

  .profile-content {
    padding: 32rpx;

    .stats-section {
      background: #FFF8DC;
      border-radius: 16rpx;
      padding: 48rpx 32rpx;
      display: flex;
      align-items: center;
      justify-content: space-around;
      margin-bottom: 24rpx;
      box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
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
        border-radius: 12rpx;
        pointer-events: none;
      }

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12rpx;

        .stat-value {
          font-size: 48rpx;
          color: #8B4513;
          font-weight: 600;
          font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
        }

        .stat-label {
          font-size: 24rpx;
          color: #8B7355;
        }
      }

      .stat-divider {
        width: 2rpx;
        height: 60rpx;
        background: #D2B48C;
      }
    }

    .menu-section {
      background: #FFF8DC;
      border-radius: 16rpx;
      overflow: hidden;
      margin-bottom: 24rpx;
      box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
      border: 2rpx solid #D2B48C;

      .menu-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 32rpx;
        border-bottom: 1rpx solid #D2B48C;

        &:last-child {
          border-bottom: none;
        }

        .menu-left {
          display: flex;
          align-items: center;
          gap: 24rpx;

          .menu-icon {
            width: 40rpx;
            height: 40rpx;
          }

          .menu-label {
            font-size: 28rpx;
            color: #5D4037;
          }
        }

        .menu-arrow {
          width: 28rpx;
          height: 28rpx;
        }

        .menu-desc {
          font-size: 24rpx;
          color: #8B7355;
        }
      }
    }

    .logout-btn {
      width: 100%;
      height: 96rpx;
      background: #FFF8DC;
      border-radius: 12rpx;
      color: #CD5C5C;
      font-size: 32rpx;
      font-weight: 600;
      border: 2rpx solid #CD5C5C;
      margin-top: 32rpx;
    }
  }
}
</style>