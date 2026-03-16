<template>
  <view class="register-page">
    <view class="register-container">
      <view class="register-header">
        <text class="title">注册账号</text>
        <text class="subtitle">开启文物探索之旅</text>
      </view>

      <view class="register-form">
        <view class="form-item">
          <view class="form-label">QQ邮箱</view>
          <input
            class="form-input"
            type="text"
            v-model="formData.email"
            placeholder="请输入QQ邮箱"
          />
        </view>

        <view class="form-item">
          <view class="form-label">验证码</view>
          <view class="code-input-wrapper">
            <input
              class="form-input code-input"
              type="number"
              v-model="formData.code"
              placeholder="请输入验证码"
              maxlength="6"
            />
            <button class="code-btn" :class="{ disabled: countdown > 0 }" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>

        <view class="form-item">
          <view class="form-label">用户名</view>
          <input
            class="form-input"
            type="text"
            v-model="formData.username"
            placeholder="请输入用户名"
            maxlength="20"
          />
        </view>

        <view class="form-item">
          <view class="form-label">设置密码</view>
          <input
            class="form-input"
            type="password"
            v-model="formData.password"
            placeholder="请设置密码（至少6位）"
          />
        </view>

        <view class="form-item">
          <view class="form-label">确认密码</view>
          <input
            class="form-input"
            type="password"
            v-model="formData.confirmPassword"
            placeholder="请再次输入密码"
          />
        </view>

        <button class="register-btn" :class="{ disabled: !canSubmit }" @click="handleRegister">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <view class="login-tip">
          <text class="tip-text">已有账号？</text>
          <text class="login-link" @click="goToLogin">立即登录</text>
        </view>
      </view>
    </view>

    <Loading :visible="loading" text="注册中..." />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { authApi } from '@/common/api/index.js'
import { validateEmail, validatePassword } from '@/common/utils/index.js'
import Loading from '@/components/Loading/Loading.vue'

const userStore = useUserStore()

const formData = ref({
  email: '',
  code: '',
  username: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const canSubmit = computed(() => {
  return (
    validateEmail(formData.value.email) &&
    formData.value.code.length === 6 &&
    formData.value.username.trim().length > 0 &&
    validatePassword(formData.value.password) &&
    formData.value.password === formData.value.confirmPassword
  )
})

const handleSendCode = async () => {
  if (!validateEmail(formData.value.email)) {
    uni.showToast({
      title: '请输入正确的QQ邮箱',
      icon: 'none'
    })
    return
  }

  if (countdown.value > 0) return

  try {
    await authApi.sendCode({
      email: formData.value.email
    })
    
    uni.showToast({
      title: '验证码已发送',
      icon: 'success'
    })

    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
    uni.showToast({
      title: '发送验证码失败，请重试',
      icon: 'none'
    })
  }
}

const handleRegister = async () => {
  if (!canSubmit.value) {
    uni.showToast({
      title: '请填写完整信息',
      icon: 'none'
    })
    return
  }

  loading.value = true

  try {
    const res = await authApi.register({
      email: formData.value.email,
      code: formData.value.code,
      username: formData.value.username,
      password: formData.value.password
    })

    userStore.setToken(res.data.token)
    if (res.data.refreshToken) {
      userStore.setRefreshToken(res.data.refreshToken)
    }
    if (res.data.user) {
      userStore.setUserInfo(res.data.user)
    }

    uni.showToast({
      title: '注册成功',
      icon: 'success'
    })

    setTimeout(() => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }, 1500)
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
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

  .register-container {
    width: 100%;

    .register-header {
      text-align: center;
      margin-bottom: 60rpx;

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

    .register-form {
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

      .register-btn {
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

      .login-tip {
        text-align: center;

        .tip-text {
          font-size: 28rpx;
          color: #5D4037;
        }

        .login-link {
          font-size: 28rpx;
          color: #8B4513;
          margin-left: 8rpx;
        }
      }
    }
  }
}
</style>