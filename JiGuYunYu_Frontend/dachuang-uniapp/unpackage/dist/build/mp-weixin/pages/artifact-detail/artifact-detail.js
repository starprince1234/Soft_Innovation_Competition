Page({
  data: {
    artifact: null,
    relatedArtifacts: [],
    loading: false
  },
  onLoad: function (options) {
    const artifactId = options.id
    if (artifactId) {
      this.loadDetail(artifactId)
    }
  },
  onReady: function () {
  },
  onShow: function () {
  },
  onHide: function () {
  },
  onUnload: function () {
  },
  onPullDownRefresh: function () {
  },
  onReachBottom: function () {
  },
  onShareAppMessage: function () {
    if (this.data.artifact) {
      return {
        title: this.data.artifact.name,
        path: `/pages/artifact-detail/artifact-detail?id=${this.data.artifact.id}`,
        imageUrl: this.data.artifact.images[0] || ''
      }
    }
  },
  loadDetail: function(artifactId) {
    this.setData({
      loading: true
    })

    try {
      setTimeout(() => {
        const mockData = {
          id: artifactId,
          name: '青花瓷瓶',
          status: 'authentic',
          images: [
            'https://neeko-copilot.bytedance.net/api/text2image?prompt=blue%20and%20white%20porcelain%20vase&size=1024x1024',
            'https://neeko-copilot.bytedance.net/api/text2image?prompt=blue%20and%20white%20porcelain%20vase%20side%20view&size=1024x1024'
          ],
          tags: ['瓷器', '明清', '青花瓷'],
          period: '明代',
          material: '瓷器',
          size: '高30cm，口径10cm',
          weight: '1.5kg',
          location: '景德镇',
          museum: '故宫博物院',
          description: '青花瓷是中国传统名瓷之一，以其独特的蓝白相间的纹饰而闻名。此瓶造型端庄，釉色莹润，纹饰精美，是明代青花瓷的代表作。',
          historicalSignificance: '青花瓷的出现标志着中国瓷器制作工艺的成熟，对后世瓷器发展产生了深远影响。',
          craftsmanship: '采用传统的青花瓷烧制工艺，经过制胎、施釉、绘画、烧制等多道工序制作而成。',
          relatedArtifacts: [
            {
              id: '2',
              name: '青花瓷盘',
              imageUrl: 'https://neeko-copilot.bytedance.net/api/text2image?prompt=blue%20and%20white%20porcelain%20plate&size=1024x1024'
            },
            {
              id: '3',
              name: '青花瓷碗',
              imageUrl: 'https://neeko-copilot.bytedance.net/api/text2image?prompt=blue%20and%20white%20porcelain%20bowl&size=1024x1024'
            }
          ]
        }
        
        this.setData({
          artifact: mockData,
          relatedArtifacts: mockData.relatedArtifacts || []
        })
      }, 1500)
    } catch (error) {
      console.error('加载详情失败:', error)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({
        loading: false
      })
    }
  },
  previewImage: function(e) {
    const index = e.currentTarget.dataset.index
    if (this.data.artifact?.images) {
      wx.previewImage({
        urls: this.data.artifact.images,
        current: index
      })
    }
  },
  handleAskAI: function() {
    if (!this.data.artifact) return

    wx.navigateTo({
      url: `/pages/chat/chat?artifactId=${this.data.artifact.id}&artifactName=${encodeURIComponent(this.data.artifact.name)}`
    })
  },
  handleShare: function() {
    wx.showShareMenu({
      withShareTicket: true
    })
  },
  goToDetail: function(e) {
    const id = e.currentTarget.dataset.id
    wx.redirectTo({
      url: `/pages/artifact-detail/artifact-detail?id=${id}`
    })
  },
  handleBack: function() {
    wx.navigateBack()
  },
  getStatusText: function(status) {
    const statusMap = {
      authentic: '真品',
      replica: '复制品',
      suspected: '疑似'
    }
    return statusMap[status] || '未知'
  },
  getStatusColor: function(status) {
    const colorMap = {
      authentic: '#4CAF50',
      replica: '#FF9800',
      suspected: '#F44336'
    }
    return colorMap[status] || '#999'
  }
})