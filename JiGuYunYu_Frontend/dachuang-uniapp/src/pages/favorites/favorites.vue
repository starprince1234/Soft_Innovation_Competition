<template>
  <view class="favorites-page">
    <view class="favorites-header">
      <text class="header-title">我的收藏</text>
    </view>
    
    <view class="favorites-content">
      <block v-if="favoritesList.length > 0">
        <view class="favorites-grid">
          <view class="favorite-item" v-for="(item, index) in favoritesList" :key="item.id || index" @click="goToDetail(item.id)">
            <image class="item-image" :src="item.imageUrl" mode="aspectFill"></image>
            <view class="item-info">
              <text class="item-title">{{ item.name || '未命名文物' }}</text>
              <view class="item-actions">
                <button class="detail-btn" @click="goToDetail(item.id)">查看详情</button>
                <button class="unfavorite-btn" @click.stop="unfavoriteItem(index)">取消收藏</button>
              </view>
            </view>
          </view>
        </view>
      </block>
      <block v-else>
        <view class="empty-state">
          <image class="empty-image" :src="emptyImage" mode="aspectFit"></image>
          <text class="empty-text">暂无收藏</text>
        </view>
      </block>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { artifactsApi } from '@/common/api/index.js'
import { resolveAssetPath } from '@/common/utils/asset-cache.js'
import { normalizeOssUrl } from '@/common/utils/oss.js'

const favoritesList = ref([])
const emptyImage = resolveAssetPath('/static/images/empty.png')

onMounted(() => {
  loadFavorites()
})

onShow(() => {
  loadFavorites()
})

const normalizeFavoriteIds = (favorites) => {
  const ids = favorites
    .map((item) => {
      if (typeof item === 'number' || typeof item === 'string') {
        return Number(item)
      }
      return Number(item?.id || item?.artifactId)
    })
    .filter((id) => Number.isFinite(id) && id > 0)

  return [...new Set(ids)]
}

const loadFavorites = async () => {
  const favorites = uni.getStorageSync('favorites') || []
  const favoriteIds = normalizeFavoriteIds(favorites)

  if (favoriteIds.length === 0) {
    favoritesList.value = []
    return
  }

  const detailResults = await Promise.all(
    favoriteIds.map(async (id) => {
      try {
        const res = await artifactsApi.getDetail(id)
        const raw = res?.data || {}
        return {
          id,
          name: raw.name || `文物 #${id}`,
          imageUrl: normalizeOssUrl(raw.thumbnailUrl || raw.imageUrl, emptyImage),
          era: raw.era,
          category: raw.category
        }
      } catch (error) {
        console.warn('加载收藏文物详情失败:', id, error)
        return {
          id,
          name: `文物 #${id}`,
          imageUrl: emptyImage,
          era: '',
          category: ''
        }
      }
    })
  )

  favoritesList.value = detailResults
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
        const target = favoritesList.value[index]
        if (!target?.id) return

        const favorites = uni.getStorageSync('favorites') || []
        const remained = favorites.filter((item) => {
          if (typeof item === 'number' || typeof item === 'string') {
            return Number(item) !== Number(target.id)
          }

          const itemId = Number(item?.id || item?.artifactId)
          return itemId !== Number(target.id)
        })

        uni.setStorageSync('favorites', remained)
        favoritesList.value.splice(index, 1)
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
              line-height: 64rpx;
              padding: 0;
              white-space: nowrap;
              word-break: keep-all;
            }

            .unfavorite-btn {
              flex: 1;
              height: 64rpx;
              background: #FFF8DC;
              color: #CD5C5C;
              border: 2rpx solid #CD5C5C;
              border-radius: 10rpx;
              font-size: 20rpx;
              line-height: 60rpx;
              padding: 0;
              white-space: nowrap;
              word-break: keep-all;
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