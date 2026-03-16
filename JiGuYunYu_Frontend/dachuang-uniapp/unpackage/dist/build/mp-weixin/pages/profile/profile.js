Page({
  data: {
    userInfo: {
      avatar: '',
      username: '',
      roles: []
    },
    userRole: '普通用户',
    stats: {
      detectCount: 0,
      chatCount: 0,
      favoriteCount: 0
    },
    cacheSize: '0 MB'
  },
  onLoad: function (options) {
    this.loadUserInfo();
  },
  onShow: function () {
    this.loadStats();
    this.loadCacheSize();
  },
  onReady: function () {
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
  },
  // 加载用户信息
  loadUserInfo: function () {
    // 模拟用户信息
    this.setData({
      userInfo: {
        avatar: '/static/images/default-avatar.png',
        username: '用户123',
        roles: ['user']
      },
      userRole: '普通用户'
    });
  },
  // 加载统计数据
  loadStats: function () {
    try {
      const history = wx.getStorageSync('detectHistory') || [];
      const chatHistory = wx.getStorageSync('chatHistory') || [];
      const favorites = wx.getStorageSync('favorites') || [];

      this.setData({
        stats: {
          detectCount: history.length,
          chatCount: chatHistory.length,
          favoriteCount: favorites.length
        }
      });
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  },
  // 加载缓存大小
  loadCacheSize: function () {
    try {
      const res = wx.getStorageInfoSync();
      const size = (res.currentSize / 1024).toFixed(2);
      this.setData({ cacheSize: `${size} MB` });
    } catch (error) {
      console.error('获取缓存大小失败:', error);
    }
  },
  // 编辑个人资料
  handleEditProfile: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 跳转到识别记录
  goToHistory: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 跳转到我的收藏
  goToFavorites: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 跳转到我的反馈
  goToFeedbackList: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 跳转到设置
  goToSettings: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 跳转到关于我们
  goToAbout: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 跳转到帮助中心
  goToHelp: function () {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },
  // 清除缓存
  handleClearCache: function () {
    wx.showModal({
      title: '提示',
      content: '确定要清除缓存吗？',
      success: (res) => {
        if (res.confirm) {
          try {
            wx.clearStorageSync();
            this.setData({
              cacheSize: '0 MB',
              stats: {
                detectCount: 0,
                chatCount: 0,
                favoriteCount: 0
              }
            });
            wx.showToast({
              title: '清除成功',
              icon: 'success'
            });
          } catch (error) {
            console.error('清除缓存失败:', error);
            wx.showToast({
              title: '清除失败',
              icon: 'none'
            });
          }
        }
      }
    });
  },
  // 退出登录
  handleLogout: function () {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '退出中...' });
            // 模拟退出登录
            setTimeout(() => {
              wx.hideLoading();
              wx.showToast({
                title: '退出成功',
                icon: 'success',
                duration: 1500,
                success: () => {
                  // 跳转到登录页
                  wx.navigateTo({ url: '/pages/login/login' });
                }
              });
            }, 1000);
          } catch (error) {
            wx.hideLoading();
            console.error('退出登录失败:', error);
            wx.showToast({
              title: '退出失败',
              icon: 'none'
            });
          }
        }
      }
    });
  }
})