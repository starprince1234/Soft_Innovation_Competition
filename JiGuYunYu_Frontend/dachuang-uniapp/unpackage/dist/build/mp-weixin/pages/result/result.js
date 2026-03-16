// pages/result/result.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    loading: false,
    artifact: null,
    taskId: ''
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.setData({
      taskId: options.taskId || ''
    })
    this.loadResult()
  },

  /**
   * 加载识别结果
   */
  loadResult: async function() {
    if (!this.data.taskId) return

    this.setData({
      loading: true
    })

    try {
      // 模拟API调用
      setTimeout(() => {
        const mockArtifact = {
          id: '1',
          name: '青花瓷瓶',
          imageUrl: 'https://neeko-copilot.bytedance.net/api/text2image?prompt=blue%20and%20white%20porcelain%20vase%20china&size=1024x1024',
          confidence: 95,
          tags: ['瓷器', '明代', '青花瓷'],
          period: '明代',
          material: '陶瓷',
          size: '高30cm',
          location: '景德镇',
          description: '青花瓷是中国传统名瓷之一，以其独特的青蓝色调闻名于世。此件青花瓷瓶造型优美，纹饰精美，是明代青花瓷的代表作。',
          historicalSignificance: '青花瓷的出现标志着中国陶瓷工艺的重大突破，对后世陶瓷发展产生了深远影响。'
        }
        this.setData({
          artifact: mockArtifact
        })
      }, 1500)
    } catch (error) {
      console.error('加载结果失败:', error)
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

  /**
   * 预览图片
   */
  previewImage: function() {
    if (this.data.artifact?.imageUrl) {
      wx.previewImage({
        urls: [this.data.artifact.imageUrl]
      })
    }
  },

  /**
   r向AI提问
   */
  handleAskAI: function() {
    if (!this.data.artifact) return

    wx.navigateTo({
      url: `/pages/chat/chat?artifactId=${this.data.artifact.id}&artifactName=${encodeURIComponent(this.data.artifact.name)}`
    })
  },

  /**
   * 分享
   */
  handleShare: function() {
    wx.showShareMenu({
      withShareTicket: true
    })
  },

  /**
   * 返回上一页
   */
  handleBack: function() {
    wx.navigateBack()
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {
    
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {
    
  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {
    
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {
    
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function() {
    if (this.data.artifact) {
      return {
        title: `我发现了一件文物：${this.data.artifact.name}`,
        path: `/pages/result/result?taskId=${this.data.taskId}`,
        imageUrl: this.data.artifact.imageUrl
      }
    }
    return {
      title: '文物识别结果',
      path: `/pages/result/result`
    }
  }
})