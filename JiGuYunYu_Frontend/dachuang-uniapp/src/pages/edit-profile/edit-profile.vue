<template>
  <view class="edit-profile-page">
    <view class="edit-profile-header">
      <text class="header-title">编辑个人资料</text>
    </view>
    
    <view class="edit-profile-content">
      <view class="avatar-section">
        <view class="avatar-wrapper" @click="handleAvatarUpload">
          <image class="avatar" :src="formData.avatar || defaultAvatarImage" mode="aspectFill"></image>
          <view class="avatar-overlay">
            <text class="avatar-text">更换头像</text>
          </view>
        </view>
      </view>
      
      <view class="form-section">
        <view class="form-item">
          <text class="form-label">用户名</text>
          <input type="text" class="form-input" v-model="formData.username" placeholder="请输入用户名" />
        </view>
        
        <view class="form-item">
          <text class="form-label">昵称</text>
          <input type="text" class="form-input" v-model="formData.nickname" placeholder="请输入昵称" />
        </view>
        
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input type="number" class="form-input" v-model="formData.phone" placeholder="请输入手机号" />
        </view>
        
        <view class="form-item">
          <text class="form-label">邮箱</text>
          <input type="email" class="form-input" v-model="formData.email" placeholder="请输入邮箱" />
        </view>
        
        <view class="form-item">
          <text class="form-label">个人简介</text>
          <textarea class="form-textarea" v-model="formData.bio" placeholder="请输入个人简介" maxlength="200"></textarea>
        </view>
      </view>
      
      <button class="save-btn" @click="handleSave">保存</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'

const userStore = useUserStore()
const defaultAvatarImage = resolveAssetPath('/static/images/default-avatar.png')

const formData = ref({
  avatar: '',
  username: '',
  nickname: '',
  phone: '',
  email: '',
  bio: ''
})

onMounted(() => {
  loadUserInfo()
})

const loadUserInfo = () => {
  const userInfo = userStore.userInfo
  if (userInfo) {
    formData.value = {
      avatar: userInfo.avatar || '',
      username: userInfo.username || '',
      nickname: userInfo.nickname || '',
      phone: userInfo.phone || '',
      email: userInfo.email || '',
      bio: userInfo.bio || ''
    }
  }
}

const handleAvatarUpload = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0]
      formData.value.avatar = tempFilePath
    },
    fail: (err) => {
      console.error('选择图片失败:', err)
      uni.showToast({
        title: '选择图片失败',
        icon: 'none'
      })
    }
  })
}

const handleSave = () => {
  if (!formData.value.username) {
    uni.showToast({
      title: '请输入用户名',
      icon: 'none'
    })
    return
  }
  
  uni.showLoading({ title: '保存中...' })
  
  setTimeout(() => {
    uni.hideLoading()
    
    const updatedUserInfo = {
      ...userStore.userInfo,
      ...formData.value
    }
    
    userStore.setUserInfo(updatedUserInfo)
    
    uni.showToast({
      title: '保存成功',
      icon: 'success'
    })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }, 1000)
}
</script>

<style lang="scss" scoped>
.edit-profile-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .edit-profile-header {
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

  .edit-profile-content {
    padding: 32rpx;

    .avatar-section {
      display: flex;
      justify-content: center;
      margin-bottom: 48rpx;

      .avatar-wrapper {
        position: relative;
        width: 200rpx;
        height: 200rpx;

        .avatar {
          width: 100%;
          height: 100%;
          border-radius: 50%;
          border: 4rpx solid #DAA520;
          box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
        }

        .avatar-overlay {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          background: rgba(139, 69, 19, 0.8);
          padding: 12rpx 0;
          text-align: center;
          border-radius: 0 0 100rpx 100rpx;

          .avatar-text {
            font-size: 20rpx;
            color: #FFF8DC;
          }
        }
      }
    }

    .form-section {
      background: #FFF8DC;
      border-radius: 16rpx;
      padding: 32rpx;
      margin-bottom: 32rpx;
      box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
      border: 2rpx solid #D2B48C;

      .form-item {
        margin-bottom: 32rpx;

        &:last-child {
          margin-bottom: 0;
        }

        .form-label {
          display: block;
          font-size: 24rpx;
          color: #5D4037;
          font-weight: 600;
          margin-bottom: 16rpx;
        }

        .form-input {
          width: 100%;
          height: 80rpx;
          background: #FFF8F0;
          border: 2rpx solid #D2B48C;
          border-radius: 12rpx;
          padding: 0 24rpx;
          font-size: 24rpx;
          color: #5D4037;
        }

        .form-textarea {
          width: 100%;
          min-height: 160rpx;
          background: #FFF8F0;
          border: 2rpx solid #D2B48C;
          border-radius: 12rpx;
          padding: 16rpx 24rpx;
          font-size: 24rpx;
          color: #5D4037;
          line-height: 1.5;
        }
      }
    }

    .save-btn {
      width: 100%;
      height: 96rpx;
      background: #8B4513;
      color: #FFF8DC;
      border-radius: 12rpx;
      font-size: 32rpx;
      font-weight: 600;
    }
  }
}
</style>