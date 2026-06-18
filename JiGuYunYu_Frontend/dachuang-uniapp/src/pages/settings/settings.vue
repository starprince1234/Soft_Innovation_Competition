<template>
  <view class="settings-page">
    <view class="settings-header">
      <text class="header-title">设置</text>
    </view>
    
    <view class="settings-content">
      <view class="settings-section">
        <text class="section-title">账户设置</text>
        <view class="setting-item" @click="handleAccountSettings">
          <text class="setting-label">账号与安全</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="setting-item" @click="handleNotificationSettings">
          <text class="setting-label">通知设置</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
      </view>
      
      <view class="settings-section">
        <text class="section-title">应用设置</text>
        <view class="setting-item" @click="handleLanguageSettings">
          <text class="setting-label">语言设置</text>
          <text class="setting-value">{{ currentLanguage }}</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="setting-item" @click="handleClearCache">
          <text class="setting-label">清除缓存</text>
          <text class="setting-value">{{ cacheSize }}</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="setting-item" @click="handleAbout">
          <text class="setting-label">关于应用</text>
          <text class="setting-value">{{ appVersion }}</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
      </view>
      
      <view class="settings-section">
        <text class="section-title">其他</text>
        <view class="setting-item" @click="handlePrivacyPolicy">
          <text class="setting-label">隐私政策</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
        <view class="setting-item" @click="handleTermsOfService">
          <text class="setting-label">服务条款</text>
          <image class="setting-arrow" :src="chevronRightIcon" mode="aspectFit"></image>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'

const currentLanguage = ref('简体中文')
const cacheSize = ref('0 MB')
const appVersion = ref('1.0.0')
const chevronRightIcon = resolveAssetPath('/static/icons/chevron-right.svg')

onMounted(() => {
  loadCacheSize()
})

const loadCacheSize = () => {
  try {
    const res = uni.getStorageInfoSync()
    const size = (res.currentSize / 1024).toFixed(2)
    cacheSize.value = `${size} MB`
  } catch (error) {
    console.error('获取缓存大小失败:', error)
  }
}

const handleAccountSettings = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

const handleNotificationSettings = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

const handleLanguageSettings = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

const handleClearCache = () => {
  uni.showModal({
    title: '提示',
    content: '确定要清除缓存吗？',
    success: (res) => {
      if (res.confirm) {
        try {
          uni.clearStorageSync()
          cacheSize.value = '0 MB'
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

const handleAbout = () => {
  uni.navigateTo({
    url: '/pages/about/about'
  })
}

const handlePrivacyPolicy = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

const handleTermsOfService = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .settings-header {
    padding: calc(var(--status-bar-height) + 32rpx) 32rpx 32rpx;
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

    .header-title {
      font-size: 40rpx;
      color: #FFF8DC;
      font-weight: 600;
      text-align: center;
      font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
    }
  }

  .settings-content {
    padding: 32rpx;

    .settings-section {
      background: #FFF8DC;
      border-radius: 16rpx;
      overflow: hidden;
      margin-bottom: 24rpx;
      box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
      border: 2rpx solid #D2B48C;

      .section-title {
        padding: 24rpx 32rpx 16rpx;
        font-size: 24rpx;
        color: #8B7355;
        font-weight: 600;
      }

      .setting-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 32rpx;
        border-top: 1rpx solid #D2B48C;

        &:first-child {
          border-top: none;
        }

        .setting-label {
          font-size: 28rpx;
          color: #5D4037;
        }

        .setting-value {
          font-size: 24rpx;
          color: #8B7355;
          margin-right: 16rpx;
        }

        .setting-arrow {
          width: 28rpx;
          height: 28rpx;
        }
      }
    }
  }
}
</style>