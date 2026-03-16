Page({
  data: {
    email: '',
    verificationCode: '',
    username: '',
    password: '',
    confirmPassword: '',
    isCountingDown: false,
    countdown: 60
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
  // 输入框输入事件
  handleInput: function (e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [field]: e.detail.value });
  },
  // 发送验证码
  sendVerificationCode: function () {
    const { email, isCountingDown } = this.data;
    
    if (!email) {
      wx.showToast({ title: '请输入邮箱', icon: 'none' });
      return;
    }
    
    if (!this.isValidEmail(email)) {
      wx.showToast({ title: '请输入有效的邮箱', icon: 'none' });
      return;
    }
    
    if (isCountingDown) return;
    
    // 模拟发送验证码
    wx.showToast({ title: '验证码已发送', icon: 'success' });
    
    // 开始倒计时
    this.setData({ isCountingDown: true });
    
    const timer = setInterval(() => {
      this.setData({
        countdown: this.data.countdown - 1
      });
      
      if (this.data.countdown <= 0) {
        clearInterval(timer);
        this.setData({
          isCountingDown: false,
          countdown: 60
        });
      }
    }, 1000);
  },
  // 验证邮箱格式
  isValidEmail: function (email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  },
  // 注册
  handleRegister: function () {
    const { email, verificationCode, username, password, confirmPassword } = this.data;
    
    // 验证表单
    if (!email) {
      wx.showToast({ title: '请输入邮箱', icon: 'none' });
      return;
    }
    
    if (!verificationCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' });
      return;
    }
    
    if (!username) {
      wx.showToast({ title: '请输入用户名', icon: 'none' });
      return;
    }
    
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }
    
    if (password !== confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' });
      return;
    }
    
    // 模拟注册成功
    wx.showToast({
      title: '注册成功',
      icon: 'success',
      duration: 1500,
      success: () => {
        // 跳转到登录页
        wx.navigateTo({ url: '/pages/login/login' });
      }
    });
  },
  // 跳转到登录页
  goToLogin: function () {
    wx.navigateTo({ url: '/pages/login/login' });
  }
})