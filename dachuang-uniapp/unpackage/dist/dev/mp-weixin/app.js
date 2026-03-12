"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const stores_user = require("./stores/user.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/history/history.js";
  "./pages/favorites/favorites.js";
  "./pages/settings/settings.js";
  "./pages/about/about.js";
  "./pages/help/help.js";
  "./pages/edit-profile/edit-profile.js";
  "./pages/login/login.js";
  "./pages/register/register.js";
  "./pages/result/result.js";
  "./pages/artifacts/artifacts.js";
  "./pages/chat/chat.js";
  "./pages/feedback/feedback.js";
  "./pages/profile/profile.js";
  "./pages/artifact-detail/artifact-detail.js";
}
const _sfc_main = {
  __name: "App",
  setup(__props) {
    common_vendor.onLaunch(() => {
      common_vendor.index.__f__("log", "at App.vue:6", "App Launch");
      const userStore = stores_user.useUserStore();
      userStore.initFromStorage();
      checkLogin();
    });
    common_vendor.onShow(() => {
      common_vendor.index.__f__("log", "at App.vue:13", "App Show");
    });
    common_vendor.onHide(() => {
      common_vendor.index.__f__("log", "at App.vue:17", "App Hide");
    });
    const checkLogin = () => {
      const token = common_vendor.index.getStorageSync("token");
      if (!token) {
        common_vendor.index.reLaunch({
          url: "/pages/login/login"
        });
      }
    };
    return () => {
    };
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  const pinia = common_vendor.createPinia();
  app.use(pinia);
  return {
    app,
    Pinia: common_vendor.Pinia
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map
