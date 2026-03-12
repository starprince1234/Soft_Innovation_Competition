<template>
  <view class="artifacts-page">
    <view class="page-header">
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          type="text"
          v-model="searchKeyword"
          placeholder="搜索文物名称、年代..."
          @confirm="handleSearch"
        />
      </view>
      <view class="filter-btn" @click="showFilter = true">
        <text class="filter-icon">🔽</text>
        <text class="filter-text">筛选</text>
      </view>
    </view>

    <scroll-view
      class="artifact-list"
      scroll-y="true"
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <view
          class="artifact-card"
          v-for="(item, index) in artifactList"
          :key="item.id || index"
          @click="goToDetail(item.id)"
        >
          <image class="artifact-image" :src="item.imageUrl" mode="aspectFill"></image>
          <view class="artifact-info">
            <text class="artifact-name">{{ item.name }}</text>
            <view class="artifact-meta">
              <text class="meta-item">{{ item.period }}</text>
              <text class="meta-item">{{ item.material }}</text>
            </view>
            <view class="artifact-tags" v-if="item.tags && item.tags.length > 0">
              <text class="tag" v-for="(tag, tagIndex) in item.tags.slice(0, 2)" :key="tagIndex">{{ tag }}</text>
            </view>
          </view>
        </view>

        <EmptyState
          v-if="artifactList.length === 0 && !loading"
          image="/static/images/empty-artifacts.png"
          text="暂无文物数据"
        />
      </view>

      <view class="load-more" v-if="hasMore">
        <text class="load-text">{{ loading ? '加载中...' : '上拉加载更多' }}</text>
      </view>

      <view class="no-more" v-if="!hasMore && artifactList.length > 0">
        <text class="no-more-text">没有更多了</text>
      </view>
    </scroll-view>

    <view class="filter-popup" :class="{ show: showFilter }" @click="showFilter = false">
      <view class="filter-content" @click.stop>
        <view class="filter-header">
          <text class="filter-title">筛选条件</text>
          <text class="close-btn" @click="showFilter = false">✕</text>
        </view>

        <view class="filter-section">
          <text class="section-label">年代</text>
          <view class="filter-options">
            <text
              class="filter-option"
              :class="{ active: filter.period === '' }"
              @click="filter.period = ''"
            >
              全部
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.period === '商周' }"
              @click="filter.period = '商周'"
            >
              商周
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.period === '秦汉' }"
              @click="filter.period = '秦汉'"
            >
              秦汉
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.period === '隋唐' }"
              @click="filter.period = '隋唐'"
            >
              隋唐
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.period === '宋元' }"
              @click="filter.period = '宋元'"
            >
              宋元
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.period === '明清' }"
              @click="filter.period = '明清'"
            >
              明清
            </text>
          </view>
        </view>

        <view class="filter-section">
          <text class="section-label">材质</text>
          <view class="filter-options">
            <text
              class="filter-option"
              :class="{ active: filter.material === '' }"
              @click="filter.material = ''"
            >
              全部
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.material === '青铜器' }"
              @click="filter.material = '青铜器'"
            >
              青铜器
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.material === '陶瓷' }"
              @click="filter.material = '陶瓷'"
            >
              陶瓷
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.material === '玉器' }"
              @click="filter.material = '玉器'"
            >
              玉器
            </text>
            <text
              class="filter-option"
              :class="{ active: filter.material === '金银器' }"
              @click="filter.material = '金银器'"
            >
              金银器
            </text>
          </view>
        </view>

        <view class="filter-actions">
          <button class="action-btn secondary" @click="resetFilter">重置</button>
          <button class="action-btn primary" @click="applyFilter">确定</button>
        </view>
      </view>
    </view>

    <Loading :visible="loading && artifactList.length === 0" text="加载中..." />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useArtifactStore } from '@/stores/artifact.js'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import Loading from '@/components/Loading/Loading.vue'
import { artifactsApi } from '@/common/api/index.js'

const artifactStore = useArtifactStore()

const artifactList = ref([])
const loading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)
const searchKeyword = ref('')
const showFilter = ref(false)

const filter = ref({
  period: '',
  material: ''
})

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

onMounted(() => {
  loadArtifacts()
})

const loadArtifacts = async (isRefresh = false) => {
  if (loading.value) return

  if (isRefresh) {
    pagination.value.page = 1
  }

  loading.value = true

  try {
    // 模拟数据，用于开发测试
    const mockData = {
      data: {
        list: [
          {
            id: 1,
            name: '司母戊鼎',
            period: '商周',
            material: '青铜器',
            imageUrl: 'https://via.placeholder.com/300x200/8B4513/FFF8DC?text=司母戊鼎',
            tags: ['国宝', '青铜器']
          },
          {
            id: 2,
            name: '兵马俑',
            period: '秦汉',
            material: '陶瓷',
            imageUrl: 'https://via.placeholder.com/300x200/8B4513/FFF8DC?text=兵马俑',
            tags: ['国宝', '陶瓷']
          },
          {
            id: 3,
            name: '唐三彩',
            period: '隋唐',
            material: '陶瓷',
            imageUrl: 'https://via.placeholder.com/300x200/8B4513/FFF8DC?text=唐三彩',
            tags: ['陶瓷', '唐代']
          },
          {
            id: 4,
            name: '清明上河图',
            period: '宋元',
            material: '纸',
            imageUrl: 'https://via.placeholder.com/300x200/8B4513/FFF8DC?text=清明上河图',
            tags: ['国宝', '绘画']
          },
          {
            id: 5,
            name: '青花瓷',
            period: '明清',
            material: '陶瓷',
            imageUrl: 'https://via.placeholder.com/300x200/8B4513/FFF8DC?text=青花瓷',
            tags: ['陶瓷', '明代']
          },
          {
            id: 6,
            name: '金缕玉衣',
            period: '秦汉',
            material: '玉器',
            imageUrl: 'https://via.placeholder.com/300x200/8B4513/FFF8DC?text=金缕玉衣',
            tags: ['国宝', '玉器']
          }
        ],
        total: 6
      }
    }

    // 模拟API请求延迟
    await new Promise(resolve => setTimeout(resolve, 500))

    const res = mockData

    if (isRefresh) {
      artifactList.value = res.data.list
    } else {
      artifactList.value = [...artifactList.value, ...res.data.list]
    }

    pagination.value.total = res.data.total
    hasMore.value = artifactList.value.length < res.data.total

    artifactStore.setArtifactList(artifactList.value)
    artifactStore.setPagination(
      pagination.value.page,
      pagination.value.pageSize,
      pagination.value.total
    )
  } catch (error) {
    console.error('加载文物列表失败:', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const loadMore = () => {
  if (!hasMore.value || loading.value) return

  pagination.value.page++
  loadArtifacts()
}

const onRefresh = () => {
  refreshing.value = true
  loadArtifacts(true)
}

const handleSearch = () => {
  loadArtifacts(true)
}

const applyFilter = () => {
  showFilter.value = false
  loadArtifacts(true)
}

const resetFilter = () => {
  filter.value = {
    period: '',
    material: ''
  }
}

const goToDetail = (id) => {
  uni.navigateTo({
    url: `/pages/artifact-detail/artifact-detail?id=${id}`
  })
}
</script>

<style lang="scss" scoped>
.artifacts-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF8F0 0%, #FAF0E6 100%);
  display: flex;
  flex-direction: column;

  .page-header {
    background: #FFF8DC;
    padding: 24rpx 32rpx;
    display: flex;
    align-items: center;
    gap: 24rpx;
    box-shadow: 0 2rpx 8rpx rgba(139, 69, 19, 0.1);
    border-bottom: 2rpx solid #D2B48C;

    .search-bar {
      flex: 1;
      height: 72rpx;
      background: #FAF0E6;
      border-radius: 36rpx;
      display: flex;
      align-items: center;
      padding: 0 24rpx;
      gap: 16rpx;
      border: 2rpx solid #D2B48C;

      .search-icon {
        font-size: 32rpx;
      }

      .search-input {
        flex: 1;
        font-size: 28rpx;
        color: #5D4037;

        &::placeholder {
          color: #8B7355;
        }
      }
    }

    .filter-btn {
      display: flex;
      align-items: center;
      gap: 8rpx;
      padding: 16rpx 24rpx;
      background: #FAF0E6;
      border-radius: 12rpx;
      border: 2rpx solid #D2B48C;

      .filter-icon {
        font-size: 24rpx;
      }

      .filter-text {
        font-size: 28rpx;
        color: #5D4037;
      }
    }
  }

  .artifact-list {
    flex: 1;
    height: calc(100vh - 120rpx);

    .list-content {
      padding: 24rpx;
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 20rpx;

      .artifact-card {
        background: #FFF8DC;
        border-radius: 16rpx;
        overflow: hidden;
        box-shadow: 0 4rpx 12rpx rgba(139, 69, 19, 0.1);
        border: 2rpx solid #D2B48C;
        position: relative;
        
        &::before {
          content: '';
          position: absolute;
          top: 8rpx;
          left: 8rpx;
          right: 8rpx;
          bottom: 8rpx;
          border: 1rpx solid rgba(218, 165, 32, 0.3);
          border-radius: 12rpx;
          pointer-events: none;
        }

        .artifact-image {
          width: 100%;
          height: 240rpx;
          border-bottom: 2rpx solid #D2B48C;
        }

        .artifact-info {
          padding: 20rpx;

          .artifact-name {
            display: block;
            font-size: 28rpx;
            color: #5D4037;
            font-weight: 600;
            margin-bottom: 12rpx;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
          }

          .artifact-meta {
            display: flex;
            gap: 16rpx;
            margin-bottom: 12rpx;

            .meta-item {
              font-size: 24rpx;
              color: #8B7355;
            }
          }

          .artifact-tags {
            display: flex;
            gap: 8rpx;
            flex-wrap: wrap;

            .tag {
              font-size: 20rpx;
              color: #8B4513;
              background: rgba(139, 69, 19, 0.1);
              padding: 4rpx 12rpx;
              border-radius: 4rpx;
              border: 1rpx solid rgba(139, 69, 19, 0.2);
            }
          }
        }
      }
    }

    .load-more,
    .no-more {
      text-align: center;
      padding: 32rpx 0;

      .load-text,
      .no-more-text {
        font-size: 24rpx;
        color: #8B7355;
      }
    }
  }

  .filter-popup {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(139, 69, 19, 0.5);
    z-index: 999;
    display: none;
    align-items: flex-end;

    &.show {
      display: flex;
    }

    .filter-content {
      width: 100%;
      max-height: 80vh;
      background: #FFF8DC;
      border-radius: 24rpx 24rpx 0 0;
      padding: 32rpx;
      overflow-y: auto;
      border-top: 4rpx solid #DAA520;

      .filter-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 32rpx;
        border-bottom: 2rpx solid #D2B48C;
        padding-bottom: 24rpx;

        .filter-title {
          font-size: 32rpx;
          color: #5D4037;
          font-weight: 600;
          font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
        }

        .close-btn {
          font-size: 40rpx;
          color: #8B7355;
        }
      }

      .filter-section {
        margin-bottom: 32rpx;

        .section-label {
          display: block;
          font-size: 28rpx;
          color: #5D4037;
          font-weight: 600;
          margin-bottom: 16rpx;
          font-family: 'PingFang SC', 'Hiragino Sans GB', serif;
        }

        .filter-options {
          display: flex;
          flex-wrap: wrap;
          gap: 16rpx;

          .filter-option {
            padding: 16rpx 32rpx;
            background: #FAF0E6;
            border-radius: 8rpx;
            font-size: 28rpx;
            color: #5D4037;
            border: 2rpx solid #D2B48C;

            &.active {
              background: #8B4513;
              color: #FFF8DC;
              border-color: #8B4513;
            }
          }
        }
      }

      .filter-actions {
        display: flex;
        gap: 24rpx;
        margin-top: 40rpx;

        .action-btn {
          flex: 1;
          height: 88rpx;
          border-radius: 12rpx;
          font-size: 28rpx;
          font-weight: 600;
          border: none;

          &.primary {
            background: #8B4513;
            color: #FFF8DC;
            border: 2rpx solid #8B4513;
          }

          &.secondary {
            background: #FAF0E6;
            color: #5D4037;
            border: 2rpx solid #D2B48C;
          }
        }
      }
    }
  }
}
</style>