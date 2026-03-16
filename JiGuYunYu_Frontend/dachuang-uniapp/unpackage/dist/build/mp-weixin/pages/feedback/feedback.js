// pages/feedback/feedback.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    formData: {
      type: 'BUG',
      textContent: '',
      rating: 5,
      images: [],
      contact: ''
    },
    loading: false,
    feedbackTypes: [
      { label: '功能异常', value: 'BUG', icon: '🐛' },
      { label: '功能建议', value: 'FEATURE_REQUEST', icon: '💡' },
      { label: '内容错误', value: 'CONTENT_ERROR', icon: '❌' },
      { label: '其他', value: 'GENERAL', icon: '📝' }
    ]
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    
  },

  /**
   * 选择反馈类型
   */
  selectType: function(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      'formData.type': type
    })
  },

  /**
   * 处理反馈内容变化
   */
  handleTextChange: function(e) {
    this.setData({
      'formData.textContent': e.detail.value
    })
  },

  /**
   * 选择评分
   */
  selectRating: function(e) {
    const rating = parseInt(e.currentTarget.dataset.rating)
    this.setData({
      'formData.rating': rating
    })
  },

  /**
   * 选择图片
   */
  chooseImage: function() {
    wx.chooseImage({
      count: 3 - this.data.formData.images.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const newImages = res.tempFilePaths.map(path => ({
          url: path
        }))
        this.setData({
          'formData.images': [...this.data.formData.images, ...newImages]
        })
      }
    })
  },

  /**
   * 删除图片
   */
  deleteImage: function(e) {
    const index = e.currentTarget.dataset.index
    const images = [...this.data.formData.images]
    images.splice(index, 1)
    this.setData({
      'formData.images': images
    })
  },

  /**
   * 处理联系方式变化
   */
  handleContactChange: function(e) {
    this.setData({
      'formData.contact': e.detail.value
    })
  },

  /**
   * 验证手机号
   */
  validatePhone: function(phone) {
    const reg = /^1[3-9]\d{9}$/
    return reg.test(phone)
  },

  /**
   * 验证邮箱
   */
  validateEmail: function(email) {
    const reg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return reg.test(email)
  },

  /**
   * 处理提交
   */
  handleSubmit: async function() {
    if (!this.data.formData.textContent.trim()) {
      wx.showToast({
        title: '请填写反馈内容',
        icon: 'none'
      })
      return
    }

    if (this.data.formData.contact && !this.validatePhone(this.data.formData.contact) && !this.validateEmail(this.data.formData.contact)) {
      wx.showToast({
        title: '请输入正确的手机号或邮箱',
        icon: 'none'
      })
      return
    }

    this.setData({
      loading: true
    })

    try {
      // 模拟API调用
      setTimeout(() => {
        wx.showToast({
          title: '提交成功',
          icon: 'success'
        })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      }, 1500)
    } catch (error) {
      console.error('提交反馈失败:', error)
      wx.showToast({
        title: error.message || '提交失败，请重试',
        icon: 'none'
      })
    } finally {
      this.setData({
        loading: false
      })
    }
  },

  /**
   * 返回上一页
   */
  handleBack: function() {
    wx.navigateBack()
  },

  /**
   * 计算是否可以提交
   */
  data: function() {
    return {
      canSubmit: this.data.formData.textContent.trim().length > 0 && this.data.formData.rating > 0
    }
  }
})