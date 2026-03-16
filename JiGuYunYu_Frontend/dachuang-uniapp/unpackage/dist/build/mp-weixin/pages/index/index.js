Page({
  data: {
    userInfo: {},
    roleName: '',
    banners: [
      {
        image: '/static/images/banner1.png',
        title: '探索中华文明',
        desc: 'AI智能识别，让文物活起来'
      },
      {
        image: '/static/images/banner2.png',
        title: '传承历史记忆',
        desc: '发现身边的文化瑰宝'
      }
    ],
    recentArtifacts: []
  },

  onLoad() {
    this.loadRecentArtifacts();
    this.getUserInfo();
  },

  getUserInfo() {
    // 这里应该从本地存储或全局状态中获取用户信息
    const userInfo = wx.getStorageSync('userInfo') || {};
    const roleName = this.getRoleName(userInfo.roles && userInfo.roles[0]);
    this.setData({
      userInfo,
      roleName
    });
  },

  getRoleName(role) {
    const roleMap = {
      'admin': '管理员',
      'user': '普通用户',
      'expert': '专家'
    };
    return roleMap[role] || '游客';
  },

  loadRecentArtifacts() {
    const history = wx.getStorageSync('detectHistory') || [];
    this.setData({
      recentArtifacts: history.slice(0, 5)
    });
  },

  handleScan() {
    wx.chooseImage({
      count: 1,
      sizeType: ['original', 'compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFilePaths[0];
        wx.showLoading({ title: '处理图片中...' });

        // 这里应该调用API进行图片识别
        setTimeout(() => {
          wx.hideLoading();
          wx.showToast({ title: '识别成功', icon: 'success' });
          // 模拟识别结果
          const mockResult = {
            id: Date.now(),
            name: '青铜器',
            image: filePath,
            description: '这是一件古代青铜器'
          };
          this.saveToHistory(mockResult);
          wx.navigateTo({
            url: `/pages/result/result?taskId=${mockResult.id}`
          });
        }, 2000);
      },
      fail: (err) => {
        console.error('选择图片失败:', err);
        wx.showToast({ title: '选择图片失败', icon: 'none' });
      }
    });
  },

  saveToHistory(artifact) {
    const history = wx.getStorageSync('detectHistory') || [];
    history.unshift(artifact);
    wx.setStorageSync('detectHistory', history.slice(0, 20));
    this.loadRecentArtifacts();
  },

  goToProfile() {
    wx.switchTab({
      url: '/pages/profile/profile'
    });
  },

  goToFeedback() {
    wx.navigateTo({
      url: '/pages/feedback/feedback'
    });
  },

  goToArtifacts() {
    wx.switchTab({
      url: '/pages/artifacts/artifacts'
    });
  },

  goToChat() {
    wx.switchTab({
      url: '/pages/chat/chat'
    });
  },

  goToHistory() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  goToAbout() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/artifact-detail/artifact-detail?id=${id}`
    });
  }
})