<template>
  <view class="feedback-page">
    <CustomNavbar title="意见反馈" :show-back="true" />

    <scroll-view class="feedback-content" scroll-y="true">
      <view class="feedback-form">
        <view class="form-section">
          <text class="section-title">反馈类型</text>
          <view class="type-selector">
            <view
              class="type-item"
              :class="{ active: formData.type === item.value }"
              v-for="item in feedbackTypes"
              :key="item.value"
              @click="formData.type = item.value"
            >
              <view class="type-icon-container">
                <text class="type-icon">{{ item.iconText }}</text>
              </view>
              <text class="type-label">{{ item.label }}</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <text class="section-title">反馈内容</text>
          <textarea
            class="feedback-textarea"
            v-model="formData.textContent"
            placeholder="请详细描述您遇到的问题或建议..."
            maxlength="500"
            :show-confirm-bar="false"
          ></textarea>
          <text class="char-count">{{ formData.textContent.length }}/500</text>
        </view>

        <view class="form-section">
          <text class="section-title">评分</text>
          <view class="rating-container">
            <text class="rating-label">您对应用的满意度</text>
            <view class="rating-stars">
              <text
                v-for="i in 5"
                :key="i"
                :class="{ active: i <= formData.rating }"
                class="star"
                @click="formData.rating = i"
              >★</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <text class="section-title">上传截图（选填）</text>
          <view class="upload-container">
            <view
              v-for="(image, index) in formData.images"
              :key="index"
              class="image-item"
            >
              <image :src="image.url" class="uploaded-image" @click="previewImage(index)"></image>
              <view class="delete-btn" @click="deleteImage(index)">×</view>
            </view>
            <view
              v-if="formData.images.length < 3"
              class="upload-btn"
              @click="handleChooseImage"
            >
              <text class="upload-icon">+</text>
              <text class="upload-text">添加截图</text>
            </view>
          </view>
          <text class="upload-tip">最多上传3张图片，每张不超过10MB</text>
        </view>

        <view class="form-section">
          <text class="section-title">联系方式（选填）</text>
          <input
            class="form-input"
            type="text"
            v-model="formData.contact"
            placeholder="请输入您的手机号或邮箱"
          />
        </view>

        <button class="submit-btn" :class="{ disabled: !canSubmit }" @click="handleSubmit">
          {{ loading ? '提交中...' : '提交反馈' }}
        </button>
      </view>
    </scroll-view>

    <Loading :visible="loading" text="提交中..." />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import CustomNavbar from '@/components/CustomNavbar/CustomNavbar.vue'
import Loading from '@/components/Loading/Loading.vue'
import { feedbackApi } from '@/common/api/index.js'
import { validatePhone, validateEmail } from '@/common/utils/index.js'
import { chooseImage, processImageForUpload } from '@/common/utils/image.js'

const formData = ref({
  type: 'BUG',
  textContent: '',
  rating: 5,
  images: [],
  contact: ''
})

const loading = ref(false)

// 修复：替换Emoji为文字/图标，避免乱码
const feedbackTypes = [
  { label: '功能异常', value: 'BUG', iconText: '故障' },
  { label: '功能建议', value: 'FEATURE_REQUEST', iconText: '建议' },
  { label: '内容错误', value: 'CONTENT_ERROR', iconText: '错误' },
  { label: '其他', value: 'GENERAL', iconText: '其他' }
]

const canSubmit = computed(() => {
  return formData.value.textContent.trim().length > 0 && formData.value.rating > 0
})

const handleChooseImage = async () => {
  try {
    const count = 3 - formData.value.images.length
    const filePaths = await chooseImage(count)

    for (const filePath of filePaths) {
      const processed = await processImageForUpload(filePath)
      
      formData.value.images.push({
        url: filePath,
        base64: processed.base64
      })
    }
  } catch (error) {
    console.error('选择图片失败:', error)
    uni.showToast({
      title: '选择图片失败',
      icon: 'none'
    })
  }
}

const deleteImage = (index) => {
  formData.value.images.splice(index, 1)
}

const previewImage = (index) => {
  const urls = formData.value.images.map(item => item.url)
  uni.previewImage({
    urls,
    current: index
  })
}

const handleSubmit = async () => {
  if (!canSubmit.value) {
    uni.showToast({
      title: '请填写反馈内容',
      icon: 'none'
    })
    return
  }

  if (formData.value.contact && !validatePhone(formData.value.contact) && !validateEmail(formData.value.contact)) {
    uni.showToast({
      title: '请输入正确的手机号或邮箱',
      icon: 'none'
    })
    return
  }

  loading.value = true

  try {
    // 修复：兼容无图片的情况，避免undefined传参
    const screenshotBase64 = formData.value.images.length > 0
      ? formData.value.images[0].base64
      : ''

    await feedbackApi.submit({
      type: formData.value.type,
      textContent: formData.value.textContent,
      rating: formData.value.rating,
      screenshotBase64
    })

    uni.showToast({
      title: '提交成功',
      icon: 'success'
    })

    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error) {
    console.error('提交反馈失败:', error)
    uni.showToast({
      title: error.message || '提交失败，请重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.feedback-page {
  min-height: 100vh;
  background-color: #F5F7FA;
  padding-top: calc(var(--status-bar-height) + 88rpx);

  .feedback-content {
    height: calc(100vh - var(--status-bar-height) - 88rpx);

    .feedback-form {
      padding: 32rpx;

      .form-section {
        margin-bottom: 36rpx;
        background: #FFFFFF;
        padding: 32rpx;
        border-radius: 16rpx;
        box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

        .section-title {
          display: block;
          font-size: 30rpx;
          color: #333333;
          font-weight: 600;
          margin-bottom: 24rpx;
          /* 修复：添加支持Emoji/特殊字符的兜底字体 */
          font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Segoe UI Emoji', sans-serif;
        }

        .type-selector {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 24rpx;

          .type-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 16rpx;
            padding: 40rpx 24rpx;
            background: #F5F5F5;
            border-radius: 16rpx;
            border: 2rpx solid #E0E0E0;
            transition: all 0.3s;
            cursor: pointer;

            &:hover {
              transform: translateY(-2rpx);
              box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
            }

            &.active {
              background: #E8F0FE;
              border-color: #1890FF;
              box-shadow: 0 4rpx 12rpx rgba(24, 144, 255, 0.2);

              .type-label {
                color: #1890FF;
              }
            }

            .type-icon-container {
              width: 80rpx;
              height: 80rpx;
              border-radius: 50%;
              background: rgba(24, 144, 255, 0.1);
              display: flex;
              align-items: center;
              justify-content: center;
            }

            .type-icon {
              font-size: 40rpx;
              font-weight: 600;
              color: #1890FF;
              /* 修复：统一字体，避免符号渲染异常 */
              font-family: 'PingFang SC', 'Segoe UI Emoji', sans-serif;
            }

            .type-label {
              font-size: 28rpx;
              color: #666666;
              font-weight: 500;
            }
          }
        }

        .feedback-textarea {
          width: 100%;
          min-height: 200rpx;
          padding: 20rpx;
          background: #F9F9F9;
          border-radius: 12rpx;
          font-size: 28rpx;
          color: #333333;
          line-height: 1.6;
          border: 1rpx solid #E0E0E0;

          &::placeholder {
            color: #999999;
          }
        }

        .char-count {
          display: block;
          text-align: right;
          font-size: 24rpx;
          color: #999999;
          margin-top: 12rpx;
        }

        .upload-tip {
          display: block;
          font-size: 24rpx;
          color: #999999;
          margin-top: 12rpx;
        }

        .form-input {
          width: 100%;
          height: 88rpx;
          background: #F9F9F9;
          border-radius: 12rpx;
          padding: 0 24rpx;
          font-size: 28rpx;
          color: #333333;
          border: 1rpx solid #E0E0E0;

          &::placeholder {
            color: #999999;
          }
        }

        .rating-container {
          display: flex;
          align-items: center;
          gap: 24rpx;

          .rating-label {
            font-size: 28rpx;
            color: #333333;
            font-weight: 500;
          }

          .rating-stars {
            display: flex;
            gap: 16rpx;

            .star {
              font-size: 48rpx;
              color: #DDDDDD;
              cursor: pointer;
              transition: color 0.2s;
              /* 修复：添加兜底字体，支持星级符号 */
              font-family: 'PingFang SC', 'Microsoft YaHei', 'Segoe UI Emoji', serif;

              &.active {
                color: #FFB800;
              }
            }
          }
        }

        .upload-container {
          display: flex;
          flex-wrap: wrap;
          gap: 24rpx;

          .image-item {
            position: relative;
            width: 160rpx;
            height: 160rpx;
            border-radius: 12rpx;
            overflow: hidden;
            box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
            transition: all 0.3s;

            &:hover {
              transform: translateY(-2rpx);
              box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
            }

            .uploaded-image {
              width: 100%;
              height: 100%;
              object-fit: cover;
            }

            .delete-btn {
              position: absolute;
              top: 8rpx;
              right: 8rpx;
              width: 36rpx;
              height: 36rpx;
              background: rgba(0, 0, 0, 0.7);
              border-radius: 50%;
              color: #FFFFFF;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 24rpx;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                background: rgba(255, 0, 0, 0.8);
                transform: scale(1.1);
              }
            }
          }

          .upload-btn {
            width: 160rpx;
            height: 160rpx;
            border: 2rpx dashed #1890FF;
            border-radius: 12rpx;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 12rpx;
            background: #F0F7FF;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
              background: #E6F7FF;
              border-color: #40A9FF;
              transform: translateY(-2rpx);
            }

            .upload-icon {
              font-size: 48rpx;
              color: #1890FF;
            }

            .upload-text {
              font-size: 24rpx;
              color: #1890FF;
            }
          }
        }
      }

      .submit-btn {
        width: 100%;
        height: 96rpx;
        background-color: #1890FF;
        border-radius: 16rpx;
        color: #FFFFFF;
        font-size: 32rpx;
        font-weight: 600;
        border: none;
        margin-top: 48rpx;
        box-shadow: 0 6rpx 16rpx rgba(24, 144, 255, 0.3);
        transition: all 0.3s;

        &:hover:not(.disabled) {
          background-color: #40A9FF;
          transform: translateY(-2rpx);
          box-shadow: 0 8rpx 20rpx rgba(24, 144, 255, 0.4);
        }

        &:active:not(.disabled) {
          transform: translateY(0);
          box-shadow: 0 4rpx 12rpx rgba(24, 144, 255, 0.3);
        }

        &.disabled {
          background-color: #E6F7FF;
          color: #91D5FF;
          box-shadow: none;
          cursor: not-allowed;
        }
      }
    }
  }
}
</style>