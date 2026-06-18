"use strict";
const common_vendor = require("../common/vendor.js");
const common_constants_index = require("../common/constants/index.js");
const useUserStore = common_vendor.defineStore("user", {
  state: () => ({
    token: "",
    userInfo: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => {
      var _a;
      return ((_a = state.userInfo) == null ? void 0 : _a.username) || "";
    },
    userRole: (state) => {
      var _a;
      return ((_a = state.userInfo) == null ? void 0 : _a.role) || "";
    },
    userId: (state) => {
      var _a;
      return ((_a = state.userInfo) == null ? void 0 : _a.id) || "";
    }
  },
  actions: {
    setToken(token) {
      this.token = token;
      common_vendor.index.setStorageSync(common_constants_index.STORAGE_KEYS.TOKEN, token);
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo;
      common_vendor.index.setStorageSync(common_constants_index.STORAGE_KEYS.USER_INFO, userInfo);
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      common_vendor.index.removeStorageSync(common_constants_index.STORAGE_KEYS.TOKEN);
      common_vendor.index.removeStorageSync(common_constants_index.STORAGE_KEYS.USER_INFO);
      common_vendor.index.reLaunch({
        url: "/pages/login/login"
      });
    },
    initFromStorage() {
      const token = common_vendor.index.getStorageSync(common_constants_index.STORAGE_KEYS.TOKEN);
      const userInfo = common_vendor.index.getStorageSync(common_constants_index.STORAGE_KEYS.USER_INFO);
      if (token)
        this.token = token;
      if (userInfo)
        this.userInfo = userInfo;
    }
  }
});
exports.useUserStore = useUserStore;
