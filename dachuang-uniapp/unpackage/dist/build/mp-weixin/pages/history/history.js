Page({
  data: {
    historyList: []
  },
  onLoad: function(options) {
    this.loadHistory();
  },
  onShow: function() {
    this.loadHistory();
  },
  loadHistory: function() {
    const history = wx.getStorageSync('detectHistory') || [];
    this.setData({
      historyList: history
    });
  },
  formatTime: function(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
  },
  goToDetail: function(e) {
    const id = e.currentTarget.dataset.id;
    if (id) {
      wx.navigateTo({
        url: `/pages/artifact-detail/artifact-detail?id=${id}`
      });
    } else {
      wx.showToast({
        title: '无法查看详情',
        icon: 'none'
      });
    }
  },
  deleteItem: function(e) {
    const index = e.currentTarget.dataset.index;
    const that = this;
    wx.showModal({
      title: '提示',
      content: '确定要删除这条记录吗？',
      success: function(res) {
        if (res.confirm) {
          const historyList = that.data.historyList;
          historyList.splice(index, 1);
          wx.setStorageSync('detectHistory', historyList);
          that.setData({
            historyList: historyList
          });
          wx.showToast({
            title: '删除成功',
            icon: 'success'
          });
        }
      }
    });
  }
});