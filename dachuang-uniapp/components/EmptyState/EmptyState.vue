<template>
  <view class="empty-state">
    <image class="empty-image" :src="image" mode="aspectFit"></image>
    <text class="empty-text">{{ text }}</text>
    <view class="empty-action" v-if="showAction">
      <slot></slot>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  image: {
    type: String,
    default: '/static/images/empty.png'
  },
  text: {
    type: String,
    default: '暂无数据'
  },
  showAction: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;

  .empty-image {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 32rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-bottom: 40rpx;
  }

  .empty-action {
    display: flex;
    gap: 24rpx;
  }
}
</style>