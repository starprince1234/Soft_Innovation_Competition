"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "settings",
  setup(__props) {
    const currentLanguage = common_vendor.ref("简体中文");
    const cacheSize = common_vendor.ref("0 MB");
    const appVersion = common_vendor.ref("1.0.0");
    common_vendor.onMounted(() => {
      loadCacheSize();
    });
    const loadCacheSize = () => {
      try {
        const res = common_vendor.index.getStorageInfoSync();
        const size = (res.currentSize / 1024).toFixed(2);
        cacheSize.value = `${size} MB`;
      } catch (error) {
        console.error("获取缓存大小失败:", error);
      }
    };
    const handleAccountSettings = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const handleNotificationSettings = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const handleLanguageSettings = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
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
              common_vendor.index.showToast({
                title: "清除成功",
                icon: "success"
              });
            } catch (error) {
              console.error("清除缓存失败:", error);
              common_vendor.index.showToast({
                title: "清除失败",
                icon: "none"
              });
            }
          }
        }
      });
    };
    const handleAbout = () => {
      common_vendor.index.navigateTo({
        url: "/pages/about/about"
      });
    };
    const handlePrivacyPolicy = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const handleTermsOfService = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(handleAccountSettings, "2d"),
        b: common_vendor.o(handleNotificationSettings, "17"),
        c: common_vendor.t(currentLanguage.value),
        d: common_vendor.o(handleLanguageSettings, "50"),
        e: common_vendor.t(cacheSize.value),
        f: common_vendor.o(handleClearCache, "da"),
        g: common_vendor.t(appVersion.value),
        h: common_vendor.o(handleAbout, "f9"),
        i: common_vendor.o(handlePrivacyPolicy, "74"),
        j: common_vendor.o(handleTermsOfService, "54")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-6d719173"]]);
wx.createPage(MiniProgramPage);
