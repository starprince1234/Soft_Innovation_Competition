<template>
  <view class="rating-selector">
    <view class="rating-content">
      <view class="rating-label">{{ label }}</view>
      <view class="rating-stars">
        <view
          class="star"
          v-for="i in 5"
          :key="i"
          @click="handleSelect(i)"
        >
          <image
            class="star-icon"
            :src="i <= value ? starFilledIcon : starOutlineIcon"
            mode="aspectFit"
          ></image>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'

const props = defineProps({
  modelValue: {
    type: Number,
    default: 0
  },
  label: {
    type: String,
    default: '评分'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])
const starOutlineIcon = resolveAssetPath('/static/icons/star-outline.svg')
const starFilledIcon = resolveAssetPath('/static/icons/star-filled.svg')

const value = computed({
  get: () => props.modelValue,
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})

const handleSelect = (rating) => {
  value.value = rating
}
</script>

<style lang="scss" scoped>
.rating-selector {
  .rating-content {
    display: flex;
    align-items: center;
    gap: 24rpx;

    .rating-label {
      font-size: 28rpx;
      color: #333333;
      font-weight: 500;
      font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
    }

    .rating-stars {
      display: flex;
      gap: 16rpx;

      .star {
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;

        .star-icon {
          width: 48rpx;
          height: 48rpx;
        }
      }
    }
  }
}
</style>