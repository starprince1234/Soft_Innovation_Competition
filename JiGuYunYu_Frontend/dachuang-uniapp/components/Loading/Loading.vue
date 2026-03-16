<template>
  <view class="loading-container" v-if="visible">
    <view class="loading-mask" @click="handleMaskClick"></view>
    <view class="loading-content">
      <view class="loading-spinner">
        <view class="spinner-dot" v-for="i in 3" :key="i"></view>
      </view>
      <text class="loading-text">{{ text }}</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: '加载中...'
  },
  maskClosable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const handleMaskClick = () => {
  if (props.maskClosable) {
    emit('close')
  }
}
</script>

<style lang="scss" scoped>
.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;

  .loading-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
  }

  .loading-content {
    position: relative;
    background: #fff;
    border-radius: 16rpx;
    padding: 60rpx 80rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 300rpx;

    .loading-spinner {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12rpx;
      margin-bottom: 24rpx;

      .spinner-dot {
        width: 16rpx;
        height: 16rpx;
        background: #C8102E;
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out both;

        &:nth-child(1) {
          animation-delay: -0.32s;
        }

        &:nth-child(2) {
          animation-delay: -0.16s;
        }
      }
    }

    .loading-text {
      font-size: 28rpx;
      color: #666;
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>