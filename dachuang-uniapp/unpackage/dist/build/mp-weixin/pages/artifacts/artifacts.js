// pages/artifacts/artifacts.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    artifactList: [],
    loading: false,
    refreshing: false,
    hasMore: true,
    searchKeyword: '',
    showFilter: false,
    filter: {
      period: '',
      material: ''
    },
    pagination: {
      page: 1,
      pageSize: 20,
      total: 0
    }
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadArtifacts()
  },

  /**
   * 加载文物列表
   */
  loadArtifacts: async function(isRefresh = false) {
    if (this.data.loading) return

    if (isRefresh) {
      this.setData({
        'pagination.page': 1
      })
    }

    this.setData({
      loading: true
    })

    try {
      const params = {
        page: this.data.pagination.page,
        pageSize: this.data.pagination.pageSize,
        keyword: this.data.searchKeyword,
        period: this.data.filter.period,
        material: this.data.filter.material
      }

      // 模拟API调用
      setTimeout(() => {
        const mockData = {
          data: {
            list: [
              {
                id: 1,
                name: '司母戊鼎',
                period: '商周',
                material: '青铜器',
                imageUrl: 'https://via.placeholder.com/300x200?text=司母戊鼎',
                tags: ['青铜器', '礼器']
              },
              {
                id: 2,
                name: '兵马俑',
                period: '秦汉',
                material: '陶瓷',
                imageUrl: 'https://via.placeholder.com/300x200?text=兵马俑',
                tags: ['陶瓷', '雕塑']
              },
              {
                id: 3,
                name: '唐三彩',
                period: '隋唐',
                material: '陶瓷',
                imageUrl: 'https://via.placeholder.com/300x200?text=唐三彩',
                tags: ['陶瓷', '工艺品']
              },
              {
                id: 4,
                name: '清明上河图',
                period: '宋元',
                material: '纸',
                imageUrl: 'https://via.placeholder.com/300x200?text=清明上河图',
                tags: ['绘画', '国宝']
              }
            ],
            total: 4
          }
        }

        if (isRefresh) {
          this.setData({
            artifactList: mockData.data.list
          })
        } else {
          this.setData({
            artifactList: [...this.data.artifactList, ...mockData.data.list]
          })
        }

        this.setData({
          'pagination.total': mockData.data.total,
          hasMore: this.data.artifactList.length < mockData.data.total
        })
      }, 1000)
    } catch (error) {
      console.error('加载文物列表失败:', error)
      uni.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({
        loading: false,
        refreshing: false
      })
    }
  },

  /**
   * 加载更多
   */
  loadMore: function() {
    if (!this.data.hasMore || this.data.loading) return

    this.setData({
      'pagination.page': this.data.pagination.page + 1
    })
    this.loadArtifacts()
  },

  /**
   * 下拉刷新
   */
  onRefresh: function() {
    this.setData({
      refreshing: true
    })
    this.loadArtifacts(true)
  },

  /**
   * 处理搜索
   */
  handleSearch: function() {
    this.loadArtifacts(true)
  },

  /**
   * 应用筛选
   */
  applyFilter: function() {
    this.setData({
      showFilter: false
    })
    this.loadArtifacts(true)
  },

  /**
   * 重置筛选
   */
  resetFilter: function() {
    this.setData({
      filter: {
        period: '',
        material: ''
      }
    })
  },

  /**
   * 处理年代筛选
   */
  handleFilterPeriod: function(period) {
    this.setData({
      'filter.period': period
    })
  },

  /**
   * 处理材质筛选
   */
  handleFilterMaterial: function(material) {
    this.setData({
      'filter.material': material
    })
  },

  /**
   * 跳转到文物详情
   */
  goToDetail: function(id) {
    uni.navigateTo({
      url: `/pages/artifact-detail/artifact-detail?id=${id}`
    })
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function() {
    this.loadMore()
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function() {
    this.onRefresh()
  }
})