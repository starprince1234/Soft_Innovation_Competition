// pages/chat/chat.js
Page({

  /**
   * 页面的初始数据
   */
  data: {
    userInfo: {
      avatar: '/static/images/default-avatar.png',
      nickname: '用户'
    },
    chatHistory: [],
    isLoading: false,
    inputMessage: '',
    scrollToView: '',
    artifactId: '',
    artifactName: '',
    quickQuestions: [
      '这件文物有什么历史意义？',
      '这件文物的制作工艺是怎样的？',
      '这件文物属于哪个朝代？',
      '这件文物的特点是什么？'
    ]
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.setData({
      artifactId: options.artifactId || '',
      artifactName: options.artifactName || ''
    })
    this.loadFromStorage()
    
    if (this.data.artifactName) {
      this.setData({
        inputMessage: `关于"${this.data.artifactName}"，我想了解更多`
      })
    }
  },

  /**
   * 从存储加载聊天历史
   */
  loadFromStorage: function() {
    const history = wx.getStorageSync('chatHistory') || []
    this.setData({
      chatHistory: history
    })
  },

  /**
   * 保存聊天历史到存储
   */
  saveToStorage: function() {
    wx.setStorageSync('chatHistory', this.data.chatHistory)
  },

  /**
   * 发送消息
   */
  handleSend: async function() {
    const message = this.data.inputMessage.trim()
    if (!message || this.data.isLoading) return

    this.setData({
      inputMessage: '',
      isLoading: true
    })

    // 添加用户消息
    const userMsg = {
      type: 'user',
      content: message,
      timestamp: Date.now()
    }
    const newHistory = [...this.data.chatHistory, userMsg]
    this.setData({
      chatHistory: newHistory
    })
    this.saveToStorage()
    this.scrollToBottom()

    try {
      // 模拟API调用
      setTimeout(() => {
        const aiMsg = {
          type: 'ai',
          content: `这是对"${message}"的回答。在实际应用中，这里会显示AI生成的文物相关知识。`,
          timestamp: Date.now()
        }
        const updatedHistory = [...this.data.chatHistory, aiMsg]
        this.setData({
          chatHistory: updatedHistory,
          isLoading: false
        })
        this.saveToStorage()
        this.scrollToBottom()
      }, 1500)
    } catch (error) {
      console.error('发送消息失败:', error)
      uni.showToast({
        title: error.message || '发送失败，请重试',
        icon: 'none'
      })
      this.setData({
        isLoading: false
      })
    }
  },

  /**
   * 处理快捷问题
   */
  handleQuickQuestion: function(e) {
    const question = e.currentTarget.dataset.question || e.target.innerText
    this.setData({
      inputMessage: question
    })
    this.handleSend()
  },

  /**
   * 清空对话历史
   */
  handleClearHistory: function() {
    wx.showModal({
      title: '提示',
      content: '确定要清空对话历史吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            chatHistory: []
          })
          this.saveToStorage()
          uni.showToast({
            title: '已清空',
            icon: 'success'
          })
        }
      }
    })
  },

  /**
   * 滚动到底部
   */
  scrollToBottom: function() {
    setTimeout(() => {
      if (this.data.chatHistory.length > 0) {
        this.setData({
          scrollToView: 'msg-' + (this.data.chatHistory.length - 1)
        })
      }
    }, 100)
  },

  /**
   * 格式化时间
   */
  formatTime: function(timestamp) {
    const date = new Date(timestamp)
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${hours}:${minutes}`
  },

  /**
   * 格式化消息内容
   */
  formatMessage: function(content) {
    // 简单的文本转节点，实际应用中可能需要更复杂的处理
    return [{ type: 'text', text: content }]
  },

  /**
   * 计算是否可以发送
   */
  get canSend() {
    return this.data.inputMessage.trim().length > 0 && !this.data.isLoading
  }
})