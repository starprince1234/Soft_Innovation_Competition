<template>
  <view class="help-page">
    <view class="help-header">
      <text class="header-title">帮助中心</text>
    </view>
    
    <view class="help-content">
      <view class="help-search">
        <view class="search-box">
          <text class="search-icon">🔍</text>
          <input type="text" class="search-input" placeholder="搜索问题" v-model="searchKeyword" />
        </view>
      </view>
      
      <view class="help-section">
        <text class="section-title">常见问题</text>
        <view class="faq-list">
          <view class="faq-item" v-for="(faq, index) in filteredFaqs" :key="index">
            <view class="faq-question" @click="toggleFaq(index)">
              <text class="question-text">{{ faq.question }}</text>
              <text class="faq-arrow">{{ faq.expanded ? '▼' : '▶' }}</text>
            </view>
            <view class="faq-answer" v-if="faq.expanded">
              <text class="answer-text">{{ faq.answer }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <view class="help-section">
        <text class="section-title">使用指南</text>
        <view class="guide-list">
          <view class="guide-item" @click="openGuide('识别指南')">
            <text class="guide-icon">📱</text>
            <text class="guide-text">如何使用文物识别功能</text>
            <text class="guide-arrow">›</text>
          </view>
          <view class="guide-item" @click="openGuide('收藏指南')">
            <text class="guide-icon">⭐</text>
            <text class="guide-text">如何收藏文物</text>
            <text class="guide-arrow">›</text>
          </view>
          <view class="guide-item" @click="openGuide('问答指南')">
            <text class="guide-icon">💬</text>
            <text class="guide-text">如何使用古韵问答功能</text>
            <text class="guide-arrow">›</text>
          </view>
        </view>
      </view>
      
      <view class="help-section">
        <text class="section-title">联系客服</text>
        <view class="contact-section">
          <view class="contact-item">
            <text class="contact-icon">📧</text>
            <text class="contact-text">邮箱：support@jiguyunyu.com</text>
          </view>
          <view class="contact-item">
            <text class="contact-icon">📞</text>
            <text class="contact-text">电话：400-123-4567</text>
          </view>
          <view class="contact-item">
            <text class="contact-icon">🕐</text>
            <text class="contact-text">工作时间：9:00-18:00</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const searchKeyword = ref('')
const faqs = ref([
  {
    question: '如何使用文物识别功能？',
    answer: '打开应用后，点击首页的"开始识别"按钮，拍摄或选择一张文物照片，系统会自动分析并给出识别结果。',
    expanded: false
  },
  {
    question: '识别结果不准确怎么办？',
    answer: '请确保拍摄的文物照片清晰，光线充足，尽量避免反光和遮挡。如果识别结果仍然不准确，您可以通过反馈功能告诉我们。',
    expanded: false
  },
  {
    question: '如何收藏文物？',
    answer: '在文物详情页面，点击右上角的收藏按钮即可将文物添加到收藏列表。',
    expanded: false
  },
  {
    question: '如何查看我的识别记录？',
    answer: '在个人中心页面，点击"识别记录"即可查看您的历史识别记录。',
    expanded: false
  },
  {
    question: '如何修改个人信息？',
    answer: '在个人中心页面，点击头像或用户名进入个人资料编辑页面，即可修改您的个人信息。',
    expanded: false
  }
])

const filteredFaqs = computed(() => {
  if (!searchKeyword.value) {
    return faqs.value
  }
  return faqs.value.filter(faq => 
    faq.question.includes(searchKeyword.value) || 
    faq.answer.includes(searchKeyword.value)
  )
})

const toggleFaq = (index) => {
  faqs.value[index].expanded = !faqs.value[index].expanded
}

const openGuide = (title) => {
  uni.showToast({
    title: `${title}功能开发中`,
    icon: 'none'
  })
}
</script>

<style lang="scss" scoped>
.help-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .help-header {
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

  .help-content {
    padding: 32rpx;

    .help-search {
      margin-bottom: 32rpx;

      .search-box {
        display: flex;
        align-items: center;
        background: #FFF8DC;
        border-radius: 12rpx;
        padding: 0 24rpx;
        height: 80rpx;
        box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
        border: 2rpx solid #D2B48C;

        .search-icon {
          font-size: 32rpx;
          margin-right: 16rpx;
          color: #8B7355;
        }

        .search-input {
          flex: 1;
          height: 100%;
          font-size: 24rpx;
          color: #5D4037;
          background: transparent;
        }
      }
    }

    .help-section {
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
        margin-bottom: 24rpx;
      }

      .faq-list {
        .faq-item {
          margin-bottom: 16rpx;

          .faq-question {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 20rpx 0;
            border-bottom: 1rpx solid #D2B48C;
            cursor: pointer;

            .question-text {
              font-size: 24rpx;
              color: #5D4037;
              flex: 1;
            }

            .faq-arrow {
              font-size: 20rpx;
              color: #8B7355;
            }
          }

          .faq-answer {
            padding: 20rpx 0;

            .answer-text {
              font-size: 22rpx;
              color: #8B7355;
              line-height: 1.5;
            }
          }
        }
      }

      .guide-list {
        .guide-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 24rpx 0;
          border-bottom: 1rpx solid #D2B48C;
          cursor: pointer;

          &:last-child {
            border-bottom: none;
          }

          .guide-icon {
            font-size: 32rpx;
            margin-right: 20rpx;
          }

          .guide-text {
            flex: 1;
            font-size: 24rpx;
            color: #5D4037;
          }

          .guide-arrow {
            font-size: 40rpx;
            color: #D2B48C;
          }
        }
      }

      .contact-section {
        .contact-item {
          display: flex;
          align-items: center;
          margin-bottom: 20rpx;

          &:last-child {
            margin-bottom: 0;
          }

          .contact-icon {
            font-size: 32rpx;
            margin-right: 20rpx;
          }

          .contact-text {
            font-size: 24rpx;
            color: #8B7355;
          }
        }
      }
    }
  }
}
</style>