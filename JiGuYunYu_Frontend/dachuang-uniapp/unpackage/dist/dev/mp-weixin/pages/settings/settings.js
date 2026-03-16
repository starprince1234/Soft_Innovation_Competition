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
        common_vendor.index.__f__("error", "at pages/settings/settings.vue:71", "获取缓存大小失败:", error);
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
              common_vendor.index.__f__("error", "at pages/settings/settings.vue:110", "清除缓存失败:", error);
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
        a: common_vendor.o(handleAccountSettings),
        b: common_vendor.o(handleNotificationSettings),
        c: common_vendor.t(currentLanguage.value),
        d: common_vendor.o(handleLanguageSettings),
        e: common_vendor.t(cacheSize.value),
        f: common_vendor.o(handleClearCache),
        g: common_vendor.t(appVersion.value),
        h: common_vendor.o(handleAbout),
        i: common_vendor.o(handlePrivacyPolicy),
        j: common_vendor.o(handleTermsOfService)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7fad0a1c"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/settings/settings.js.map
