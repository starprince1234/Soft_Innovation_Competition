<template>
  <view class="favorites-page">
    <view class="favorites-header">
      <text class="header-title">我的收藏</text>
    </view>
    
    <view class="favorites-content">
      <block v-if="favoritesList.length > 0">
        <view class="favorites-grid">
          <view class="favorite-item" v-for="(item, index) in favoritesList" :key="index">
            <image class="item-image" :src="item.image" mode="aspectFill"></image>
            <view class="item-info">
              <text class="item-title">{{ item.name || '未命名文物' }}</text>
              <view class="item-actions">
                <button class="detail-btn" @click="goToDetail(item.id)">查看详情</button>
                <button class="unfavorite-btn" @click="unfavoriteItem(index)">取消收藏</button>
              </view>
            </view>
          </view>
        </view>
      </block>
      <block v-else>
        <view class="empty-state">
          <image class="empty-image" src="/static/images/empty.png" mode="aspectFit"></image>
          <text class="empty-text">暂无收藏</text>
        </view>
      </block>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const favoritesList = ref([])

onMounted(() => {
  loadFavorites()
})

const loadFavorites = () => {
  const favorites = uni.getStorageSync('favorites') || []
  favoritesList.value = favorites
}

const goToDetail = (id) => {
  if (id) {
    uni.navigateTo({
      url: `/pages/artifact-detail/artifact-detail?id=${id}`
    })
  } else {
    uni.showToast({
      title: '无法查看详情',
      icon: 'none'
    })
  }
}

const unfavoriteItem = (index) => {
  uni.showModal({
    title: '提示',
    content: '确定要取消收藏吗？',
    success: (res) => {
      if (res.confirm) {
        favoritesList.value.splice(index, 1)
        uni.setStorageSync('favorites', favoritesList.value)
        uni.showToast({
          title: '取消收藏成功',
          icon: 'success'
        })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.favorites-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .favorites-header {
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

  .favorites-content {
    padding: 32rpx;

    .favorites-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 24rpx;

      .favorite-item {
        background: #FFF8DC;
        border-radius: 16rpx;
        overflow: hidden;
        box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
        border: 2rpx solid #D2B48C;

        .item-image {
          width: 100%;
          height: 240rpx;
          border-bottom: 2rpx solid #D2B48C;
        }

        .item-info {
          padding: 20rpx;

          .item-title {
            font-size: 24rpx;
            color: #5D4037;
            font-weight: 600;
            margin-bottom: 16rpx;
            line-height: 1.4;
          }

          .item-actions {
            display: flex;
            gap: 12rpx;

            .detail-btn {
              flex: 1;
              height: 64rpx;
              background: #8B4513;
              color: #FFF8DC;
              border-radius: 10rpx;
              font-size: 20rpx;
            }

            .unfavorite-btn {
              flex: 1;
              height: 64rpx;
              background: #FFF8DC;
              color: #CD5C5C;
              border: 2rpx solid #CD5C5C;
              border-radius: 10rpx;
              font-size: 20rpx;
            }
          }
        }
      }
    }

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
        color: #8B7355;
      }
    }
  }
}
</style>