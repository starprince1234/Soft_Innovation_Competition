<template>
  <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px', backgroundColor: bgColor }">
    <view class="navbar-content" :style="{ height: navbarHeight + 'px' }">
      <view class="navbar-left" @click="handleBack">
        <image class="icon-back" v-if="showBack" :src="chevronRightIcon" mode="aspectFit"></image>
      </view>
      <view class="navbar-title">
        <text class="title-text">{{ title }}</text>
      </view>
      <view class="navbar-right">
        <slot name="right"></slot>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  },
  bgColor: {
    type: String,
    default: '#FFFFFF'
  }
})

const emit = defineEmits(['back'])

const statusBarHeight = ref(0)
const navbarHeight = ref(44)
const chevronRightIcon = resolveAssetPath('/static/icons/chevron-right.svg')

onMounted(() => {
  const systemInfo = uni.getSystemInfoSync()
  statusBarHeight.value = systemInfo.statusBarHeight || 0
})

const handleBack = () => {
  emit('back')
  uni.navigateBack({
    delta: 1
  })
}
</script>

<style lang="scss" scoped>
.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

  .navbar-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 32rpx;

    .navbar-left {
      width: 80rpx;
      height: 100%;
      display: flex;
      align-items: center;

      .icon-back {
        width: 34rpx;
        height: 34rpx;
        transform: rotate(180deg);
      }
    }

    .navbar-title {
      flex: 1;
      text-align: center;

      .title-text {
        font-size: 36rpx;
        font-weight: 600;
        color: #333;
      }
    }

    .navbar-right {
      width: 80rpx;
      display: flex;
      align-items: center;
      justify-content: flex-end;
    }
  }
}
</style>