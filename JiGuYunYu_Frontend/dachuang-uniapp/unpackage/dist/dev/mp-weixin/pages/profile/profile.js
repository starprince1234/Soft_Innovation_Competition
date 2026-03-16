"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_user = require("../../stores/user.js");
const common_utils_index = require("../../common/utils/index.js");
const common_api_index = require("../../common/api/index.js");
const _sfc_main = {
  __name: "profile",
  setup(__props) {
    const userStore = stores_user.useUserStore();
    const userInfo = common_vendor.computed(() => userStore.userInfo);
    const stats = common_vendor.ref({
      detectCount: 0,
      chatCount: 0,
      favoriteCount: 0
    });
    const cacheSize = common_vendor.ref("0 MB");
    common_vendor.onMounted(() => {
      loadStats();
      loadCacheSize();
    });
    const loadStats = () => {
      const history = common_vendor.index.getStorageSync("detectHistory") || [];
      const chatHistory = common_vendor.index.getStorageSync("chatHistory") || [];
      const favorites = common_vendor.index.getStorageSync("favorites") || [];
      stats.value = {
        detectCount: history.length,
        chatCount: chatHistory.length,
        favoriteCount: favorites.length
      };
    };
    const loadCacheSize = () => {
      try {
        const res = common_vendor.index.getStorageInfoSync();
        const size = (res.currentSize / 1024).toFixed(2);
        cacheSize.value = `${size} MB`;
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/profile/profile.vue:139", "获取缓存大小失败:", error);
      }
    };
    const handleEditProfile = () => {
      common_vendor.index.navigateTo({
        url: "/pages/edit-profile/edit-profile"
      });
    };
    const goToHistory = () => {
      common_vendor.index.navigateTo({
        url: "/pages/history/history"
      });
    };
    const goToFavorites = () => {
      common_vendor.index.navigateTo({
        url: "/pages/favorites/favorites"
      });
    };
    const goToFeedbackList = () => {
      common_vendor.index.navigateTo({
        url: "/pages/feedback/feedback"
      });
    };
    const goToSettings = () => {
      common_vendor.index.navigateTo({
        url: "/pages/settings/settings"
      });
    };
    const goToAbout = () => {
      common_vendor.index.navigateTo({
        url: "/pages/about/about"
      });
    };
    const goToHelp = () => {
      common_vendor.index.navigateTo({
        url: "/pages/help/help"
      });
    };
    const handleClearCache = () => {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要清除缓存吗？",
        success: (res) => {
          if (res.confirm) {
            try {
              common_vendor.index.clearStorageSync();
              cacheSize.value = "0 MB";
              stats.value = {
                detectCount: 0,
                chatCount: 0,
                favoriteCount: 0
              };
              common_vendor.index.showToast({
                title: "清除成功",
                icon: "success"
              });
            } catch (error) {
              common_vendor.index.__f__("error", "at pages/profile/profile.vue:204", "清除缓存失败:", error);
              common_vendor.index.showToast({
                title: "清除失败",
                icon: "none"
              });
            }
          }
        }
      });
    };
    const handleLogout = async () => {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要退出登录吗？",
        success: async (res) => {
          if (res.confirm) {
            try {
              common_vendor.index.showLoading({ title: "退出中..." });
              await common_api_index.authApi.logout();
              common_vendor.index.hideLoading();
              userStore.logout();
            } catch (error) {
              common_vendor.index.hideLoading();
              common_vendor.index.__f__("error", "at pages/profile/profile.vue:228", "登出请求失败:", error);
              userStore.logout();
            }
          }
        }
      });
    };
    return (_ctx, _cache) => {
      var _a, _b, _c, _d;
      return {
        a: ((_a = userInfo.value) == null ? void 0 : _a.avatar) || "/static/images/default-avatar.png",
        b: common_vendor.t(((_b = userInfo.value) == null ? void 0 : _b.username) || "未设置"),
        c: common_vendor.t(common_vendor.unref(common_utils_index.getRoleName)((_d = (_c = userInfo.value) == null ? void 0 : _c.roles) == null ? void 0 : _d[0])),
        d: common_vendor.o(handleEditProfile),
        e: common_vendor.t(stats.value.detectCount),
        f: common_vendor.t(stats.value.chatCount),
        g: common_vendor.t(stats.value.favoriteCount),
        h: common_vendor.o(goToHistory),
        i: common_vendor.o(goToFavorites),
        j: common_vendor.o(goToFeedbackList),
        k: common_vendor.o(goToSettings),
        l: common_vendor.o(goToAbout),
        m: common_vendor.o(goToHelp),
        n: common_vendor.t(cacheSize.value),
        o: common_vendor.o(handleClearCache),
        p: common_vendor.o(handleLogout)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-dd383ca2"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/profile/profile.js.map
