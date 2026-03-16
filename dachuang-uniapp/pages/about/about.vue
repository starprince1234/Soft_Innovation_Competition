<template>
  <view class="about-page">
    <view class="about-header">
      <text class="header-title">关于我们</text>
    </view>
    
    <view class="about-content">
      <view class="app-info">
        <image class="app-logo" src="/static/images/logo.png" mode="aspectFit"></image>
        <text class="app-name">稽古云语</text>
        <text class="app-version">版本 1.0.0</text>
      </view>
      
      <view class="about-section">
        <text class="section-title">应用介绍</text>
        <text class="section-content">
          稽古云语是一款专注于文物鉴识的智能应用，通过先进的AI技术，为用户提供快速、准确的文物识别和信息查询服务。
          我们致力于传承和弘扬中华优秀传统文化，让更多人了解和欣赏文物的魅力。
        </text>
      </view>
      
      <view class="about-section">
        <text class="section-title">核心功能</text>
        <view class="feature-list">
          <view class="feature-item">
            <text class="feature-icon">🔍</text>
            <text class="feature-text">文物识别</text>
          </view>
          <view class="feature-item">
            <text class="feature-icon">📚</text>
            <text class="feature-text">宝库珍藏</text>
          </view>
          <view class="feature-item">
            <text class="feature-icon">💬</text>
            <text class="feature-text">古韵问答</text>
          </view>
          <view class="feature-item">
            <text class="feature-icon">⭐</text>
            <text class="feature-text">我的收藏</text>
          </view>
        </view>
      </view>
      
      <view class="about-section">
        <text class="section-title">联系我们</text>
        <view class="contact-list">
          <view class="contact-item">
            <text class="contact-label">邮箱</text>
            <text class="contact-value">support@jiguyunyu.com</text>
          </view>
          <view class="contact-item">
            <text class="contact-label">电话</text>
            <text class="contact-value">400-123-4567</text>
          </view>
        </view>
      </view>
      
      <view class="about-section">
        <text class="section-title">相关链接</text>
        <view class="link-list">
          <view class="link-item" @click="openLink('https://www.jiguyunyu.com')">
            <text class="link-text">官方网站</text>
            <text class="link-arrow">›</text>
          </view>
          <view class="link-item" @click="openLink('https://www.jiguyunyu.com/privacy')">
            <text class="link-text">隐私政策</text>
            <text class="link-arrow">›</text>
          </view>
          <view class="link-item" @click="openLink('https://www.jiguyunyu.com/terms')">
            <text class="link-text">服务条款</text>
            <text class="link-arrow">›</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
const openLink = (url) => {
  uni.openURL({
    url: url,
    success: () => {
      console.log('打开链接成功')
    },
    fail: (err) => {
      console.error('打开链接失败:', err)
      uni.showToast({
        title: '打开链接失败',
        icon: 'none'
      })
    }
  })
}
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .about-header {
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

  .about-content {
    padding: 32rpx;

    .app-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 48rpx;

      .app-logo {
        width: 200rpx;
        height: 200rpx;
        margin-bottom: 24rpx;
        border-radius: 24rpx;
        border: 4rpx solid #DAA520;
        box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
      }

      .app-name {
        font-size: 36rpx;
        color: #5D4037;
        font-weight: 600;
        margin-bottom: 12rpx;
        font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
      }

      .app-version {
        font-size: 24rpx;
        color: #8B7355;
      }
    }

    .about-section {
      background: #FFF8DC;
      border-radius: 16rpx;
      padding: 32rpx;
      margin-bottom: 24rpx;
      box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
      border: 2rpx solid #D2B48C;

      .section-title {
        font-size: 28rpx;
        color: #5D4037;
        font-weight: 600;
        margin-bottom: 20rpx;
      }

      .section-content {
        font-size: 24rpx;
        color: #8B7355;
        line-height: 1.5;
      }

      .feature-list {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 24rpx;

        .feature-item {
          display: flex;
          align-items: center;
          gap: 16rpx;

          .feature-icon {
            font-size: 36rpx;
          }

          .feature-text {
            font-size: 24rpx;
            color: #8B7355;
          }
        }
      }

      .contact-list {
        .contact-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16rpx;

          &:last-child {
            margin-bottom: 0;
          }

          .contact-label {
            font-size: 24rpx;
            color: #5D4037;
          }

          .contact-value {
            font-size: 24rpx;
            color: #8B7355;
          }
        }
      }

      .link-list {
        .link-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16rpx 0;
          border-bottom: 1rpx solid #D2B48C;

          &:last-child {
            border-bottom: none;
          }

          .link-text {
            font-size: 24rpx;
            color: #5D4037;
          }

          .link-arrow {
            font-size: 40rpx;
            color: #D2B48C;
          }
        }
      }
    }
  }
}
</style>