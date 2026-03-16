<template>
  <view class="image-uploader">
    <view class="upload-list">
      <view
        class="upload-item"
        v-for="(item, index) in fileList"
        :key="index"
      >
        <image class="upload-image" :src="item.url" mode="aspectFill" @click="previewImage(index)"></image>
        <view class="delete-btn" @click="handleDelete(index)">
          <text class="delete-icon">×</text>
        </view>
        <view class="upload-status" v-if="item.status === 'uploading'">
          <text class="status-text">上传中...</text>
        </view>
      </view>

      <view
        class="upload-btn"
        v-if="fileList.length < maxCount"
        @click="handleChooseImage"
      >
        <text class="upload-icon">+</text>
        <text class="upload-text">{{ placeholder }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { chooseImage, processImageForUpload } from '@/common/utils/image.js'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxCount: {
    type: Number,
    default: 1
  },
  placeholder: {
    type: String,
    default: '上传图片'
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'upload'])

const fileList = ref([...props.modelValue])

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  fileList.value = [...newValue]
}, { deep: true })

const handleChooseImage = async () => {
  try {
    const count = props.maxCount - fileList.value.length
    const filePaths = await chooseImage(count)

    for (const filePath of filePaths) {
      const processed = await processImageForUpload(filePath)
      
      const fileItem = {
        url: filePath,
        base64: processed.base64,
        status: 'uploading'
      }
      
      fileList.value.push(fileItem)
      
      emit('upload', fileItem, (success) => {
        if (success) {
          fileItem.status = 'done'
        } else {
          fileItem.status = 'error'
        }
        emitChange()
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

const handleDelete = (index) => {
  fileList.value.splice(index, 1)
  emitChange()
}

const previewImage = (index) => {
  const urls = fileList.value.map(item => item.url)
  uni.previewImage({
    urls,
    current: index
  })
}

const emitChange = () => {
  emit('update:modelValue', fileList.value)
  emit('change', fileList.value)
}
</script>

<style lang="scss" scoped>
.image-uploader {
  .upload-list {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;

    .upload-item {
      position: relative;
      width: 200rpx;
      height: 200rpx;
      border-radius: 12rpx;
      overflow: hidden;

      .upload-image {
        width: 100%;
        height: 100%;
      }

      .delete-btn {
        position: absolute;
        top: 8rpx;
        right: 8rpx;
        width: 40rpx;
        height: 40rpx;
        background: rgba(0, 0, 0, 0.6);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        .delete-icon {
          color: #fff;
          font-size: 32rpx;
          line-height: 1;
        }
      }

      .upload-status {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        background: rgba(0, 0, 0, 0.6);
        padding: 8rpx;
        text-align: center;

        .status-text {
          color: #fff;
          font-size: 20rpx;
        }
      }
    }

    .upload-btn {
      width: 200rpx;
      height: 200rpx;
      border: 2rpx dashed #DDDDDD;
      border-radius: 12rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #F9F9F9;

      .upload-icon {
        font-size: 60rpx;
        color: #999;
        line-height: 1;
        margin-bottom: 12rpx;
      }

      .upload-text {
        font-size: 24rpx;
        color: #999;
      }
    }
  }
}
</style>