Page({
  data: {
  },
  onLoad: function (options) {
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
  },
  handleWechatLogin: function () {
    wx.login({
      success: (res) => {
        if (res.code) {
          // 这里可以发送code到后端进行登录验证
          console.log('微信登录code:', res.code);
          // 模拟登录成功，跳转到首页
          wx.switchTab({ url: '/pages/index/index' });
        } else {
          console.error('微信登录失败:', res.errMsg);
          wx.showToast({ title: '登录失败', icon: 'none' });
        }
      },
      fail: (err) => {
        console.error('微信登录失败:', err);
        wx.showToast({ title: '登录失败', icon: 'none' });
      }
    });
  },
  goToRegister: function () {
    wx.navigateTo({ url: '/pages/register/register' });
  }
})