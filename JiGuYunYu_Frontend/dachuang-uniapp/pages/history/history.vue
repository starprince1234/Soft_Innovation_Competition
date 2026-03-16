<template>
  <view class="history-page">
    <view class="history-header">
      <text class="header-title">识别记录</text>
    </view>
    
    <view class="history-content">
      <block v-if="historyList.length > 0">
        <view class="history-item" v-for="(item, index) in historyList" :key="index">
          <view class="item-header">
            <text class="item-title">{{ item.name || '未命名文物' }}</text>
            <text class="item-time">{{ formatTime(item.timestamp) }}</text>
          </view>
          <view class="item-content">
            <image class="item-image" :src="item.image" mode="aspectFill"></image>
            <view class="item-info">
              <text class="item-desc">{{ item.description || '暂无描述' }}</text>
            </view>
          </view>
          <view class="item-footer">
            <button class="detail-btn" @click="goToDetail(item.id)">查看详情</button>
            <button class="delete-btn" @click="deleteItem(index)">删除记录</button>
          </view>
        </view>
      </block>
      <block v-else>
        <view class="empty-state">
          <image class="empty-image" src="/static/images/empty.png" mode="aspectFit"></image>
          <text class="empty-text">暂无识别记录</text>
        </view>
      </block>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const historyList = ref([])

onMounted(() => {
  loadHistory()
})

const loadHistory = () => {
  const history = uni.getStorageSync('detectHistory') || []
  historyList.value = history
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
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

const deleteItem = (index) => {
  uni.showModal({
    title: '提示',
    content: '确定要删除这条记录吗？',
    success: (res) => {
      if (res.confirm) {
        historyList.value.splice(index, 1)
        uni.setStorageSync('detectHistory', historyList.value)
        uni.showToast({
          title: '删除成功',
          icon: 'success'
        })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.history-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);

  .history-header {
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

  .history-content {
    padding: 32rpx;

    .history-item {
      background: #FFF8DC;
      border-radius: 16rpx;
      padding: 24rpx;
      margin-bottom: 24rpx;
      box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
      border: 2rpx solid #D2B48C;

      .item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16rpx;

        .item-title {
          font-size: 28rpx;
          color: #5D4037;
          font-weight: 600;
        }

        .item-time {
          font-size: 20rpx;
          color: #8B7355;
        }
      }

      .item-content {
        display: flex;
        gap: 20rpx;
        margin-bottom: 20rpx;

        .item-image {
          width: 160rpx;
          height: 160rpx;
          border-radius: 12rpx;
          border: 2rpx solid #D2B48C;
        }

        .item-info {
          flex: 1;
          display: flex;
          align-items: center;

          .item-desc {
            font-size: 24rpx;
            color: #8B7355;
            line-height: 1.5;
          }
        }
      }

      .item-footer {
        display: flex;
        gap: 16rpx;
        justify-content: flex-end;

        .detail-btn {
          flex: 1;
          height: 72rpx;
          background: #8B4513;
          color: #FFF8DC;
          border-radius: 12rpx;
          font-size: 24rpx;
        }

        .delete-btn {
          flex: 1;
          height: 72rpx;
          background: #FFF8DC;
          color: #CD5C5C;
          border: 2rpx solid #CD5C5C;
          border-radius: 12rpx;
          font-size: 24rpx;
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