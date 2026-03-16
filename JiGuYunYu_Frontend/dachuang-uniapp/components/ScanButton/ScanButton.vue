<template>
  <view class="scan-button" @click="handleScan">
    <view class="scan-outer">
      <view class="scan-inner">
        <view class="scan-corner scan-top-left"></view>
        <view class="scan-corner scan-top-right"></view>
        <view class="scan-corner scan-bottom-left"></view>
        <view class="scan-corner scan-bottom-right"></view>
        <view class="scan-icon">
          <text class="icon-text">📷</text>
        </view>
      </view>
    </view>
    <text class="scan-text">{{ text }}</text>
  </view>
</template>

<script setup>
const props = defineProps({
  text: {
    type: String,
    default: '点击拍照识别'
  }
})

const emit = defineEmits('scan')

const handleScan = () => {
  emit('scan')
}
</script>

<style lang="scss" scoped>
.scan-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;

  .scan-outer {
    width: 200rpx;
    height: 200rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #C8102E 0%, #8B0A1F 100%);
    box-shadow: 0 8rpx 24rpx rgba(200, 16, 46, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;

    .scan-inner {
      width: 180rpx;
      height: 180rpx;
      border-radius: 50%;
      background: #fff;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;

      .scan-corner {
        position: absolute;
        width: 40rpx;
        height: 40rpx;
        border: 4rpx solid #C8102E;

        &.scan-top-left {
          top: 20rpx;
          left: 20rpx;
          border-right: none;
          border-bottom: none;
          border-top-left-radius: 8rpx;
        }

        &.scan-top-right {
          top: 20rpx;
          right: 20rpx;
          border-left: none;
          border-bottom: none;
          border-top-right-radius: 8rpx;
        }

        &.scan-bottom-left {
          bottom: 20rpx;
          left: 20rpx;
          border-right: none;
          border-top: none;
          border-bottom-left-radius: 8rpx;
        }

        &.scan-bottom-right {
          bottom: 20rpx;
          right: 20rpx;
          border-left: none;
          border-top: none;
          border-bottom-right-radius: 8rpx;
        }
      }

      .scan-icon {
        .icon-text {
          font-size: 80rpx;
        }
      }
    }
  }

  .scan-text {
    font-size: 28rpx;
    color: #666;
  }
}
</style>