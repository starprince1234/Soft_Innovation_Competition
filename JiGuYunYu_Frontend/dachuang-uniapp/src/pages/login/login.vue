<template>
  <view class="login-page">
    <view class="login-container">
      <view class="login-header">
        <image class="logo" :src="logoImage" mode="aspectFit"></image>
        <text class="title">稽古云语</text>
        <text class="subtitle">探索中华文明，传承历史记忆</text>
      </view>

      <view class="login-form">
        <view class="form-item">
          <view class="form-label">用户名</view>
          <input
            class="form-input"
            type="text"
            v-model="formData.username"
            placeholder="请输入用户名"
          />
        </view>

        <view class="form-item">
          <view class="form-label">密码</view>
          <input
            class="form-input"
            type="password"
            v-model="formData.password"
            placeholder="请输入密码"
          />
        </view>

        <button class="email-login-btn" :class="{ disabled: !canSubmit }" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <view class="register-tip">
          <text class="tip-text">还没有账号？</text>
          <text class="register-link" @click="goToRegister">立即注册</text>
        </view>
      </view>

      <view class="login-footer">
        <text class="agreement-text">登录即表示同意</text>
        <text class="agreement-link">《用户协议》</text>
        <text class="agreement-text">和</text>
        <text class="agreement-link">《隐私政策》</text>
      </view>
    </view>

    <Loading :visible="loading" text="登录中..." />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { authApi, userApi } from '@/common/api/index.js'
import Loading from '@/components/Loading/Loading.vue'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'

const userStore = useUserStore()
const logoImage = resolveAssetPath('/static/images/logo.png')

const loading = ref(false)

const formData = ref({
  username: '',
  password: ''
})

const canSubmit = computed(() => {
  return formData.value.username.trim().length > 0 && formData.value.password.length >= 6
})

const handleLogin = async () => {
  if (!canSubmit.value) {
    uni.showToast({
      title: '请输入用户名和密码',
      icon: 'none'
    })
    return
  }

  loading.value = true

  try {
    const authRes = await authApi.login({
      username: formData.value.username,
      password: formData.value.password
    })

    userStore.setToken(authRes.data.token)

    const userRes = await userApi.getMe()
    userStore.setUserInfo(userRes.data)

    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })

    setTimeout(() => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }, 1500)
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  uni.navigateTo({
    url: '/pages/register/register'
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #8B4513 0%, #65320F 100%);
  padding: 0 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
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

  .login-container {
    width: 100%;

    .login-header {
      text-align: center;
      margin-bottom: 60rpx;

      .logo {
        width: 160rpx;
        height: 160rpx;
        margin-bottom: 32rpx;
      }

      .title {
        display: block;
        font-size: 56rpx;
        font-weight: bold;
        color: #FFF8DC;
        margin-bottom: 16rpx;
        font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
      }

      .subtitle {
        display: block;
        font-size: 28rpx;
        color: rgba(255, 248, 220, 0.8);
      }
    }

    .login-tabs {
      display: flex;
      background: #FFF8DC;
      border-radius: 12rpx;
      margin-bottom: 32rpx;
      overflow: hidden;
      border: 2rpx solid #D2B48C;

      .tab-item {
        flex: 1;
        padding: 24rpx 0;
        text-align: center;
        border-bottom: 4rpx solid transparent;
        transition: all 0.3s;

        &.active {
          border-bottom-color: #8B4513;
          background: rgba(139, 69, 19, 0.05);
        }

        .tab-text {
          font-size: 28rpx;
          color: #5D4037;
        }
      }
    }

    .login-form {
      background: #FFF8DC;
      border-radius: 24rpx;
      padding: 60rpx 48rpx;
      box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.2);
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

      .form-item {
        margin-bottom: 32rpx;

        .form-label {
          font-size: 28rpx;
          color: #5D4037;
          margin-bottom: 16rpx;
          display: block;
        }

        .form-input {
          width: 100%;
          height: 88rpx;
          border: 2rpx solid #D2B48C;
          border-radius: 12rpx;
          padding: 0 24rpx;
          font-size: 28rpx;
          color: #5D4037;
          background: #FAF0E6;

          &::placeholder {
            color: #8B7355;
          }
        }

        .code-input-wrapper {
          display: flex;
          gap: 16rpx;

          .code-input {
            flex: 1;
          }

          .code-btn {
            width: 200rpx;
            height: 88rpx;
            background: #8B4513;
            border-radius: 12rpx;
            color: #FFF8DC;
            font-size: 24rpx;
            border: 2rpx solid #8B4513;
            display: flex;
            align-items: center;
            justify-content: center;
            white-space: nowrap;

            &.disabled {
              background: #D2B48C;
              border-color: #D2B48C;
            }
          }
        }
      }

      .wechat-login-btn {
        width: 100%;
        height: 96rpx;
        background: linear-gradient(135deg, #07C160 0%, #05A850 100%);
        border-radius: 12rpx;
        color: white;
        font-size: 32rpx;
        font-weight: 600;
        border: 2rpx solid #07C160;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 32rpx;
      }

      .email-login-btn {
        width: 100%;
        height: 96rpx;
        background: linear-gradient(135deg, #8B4513 0%, #65320F 100%);
        border-radius: 12rpx;
        color: #FFF8DC;
        font-size: 32rpx;
        font-weight: 600;
        border: 2rpx solid #8B4513;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 32rpx;

        &.disabled {
          opacity: 0.5;
        }
      }

      .register-tip {
        text-align: center;

        .tip-text {
          font-size: 28rpx;
          color: #5D4037;
        }

        .register-link {
          font-size: 28rpx;
          color: #8B4513;
          margin-left: 8rpx;
        }
      }
    }

    .login-footer {
      text-align: center;
      margin-top: 48rpx;

      .agreement-text {
        font-size: 24rpx;
        color: rgba(255, 248, 220, 0.8);
      }

      .agreement-link {
        font-size: 24rpx;
        color: #FFF8DC;
        text-decoration: underline;
      }
    }
  }
}
</style>