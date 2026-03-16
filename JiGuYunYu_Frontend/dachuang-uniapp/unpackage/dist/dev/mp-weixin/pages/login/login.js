"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const stores_user = require("../../stores/user.js");
const common_api_index = require("../../common/api/index.js");
if (!Math) {
  Loading();
}
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "login",
  setup(__props) {
    const userStore = stores_user.useUserStore();
    const loading = common_vendor.ref(false);
    const handleWechatLogin = async () => {
      loading.value = true;
      try {
        common_vendor.index.login({
          provider: "weixin",
          success: async (loginRes) => {
            common_vendor.index.__f__("log", "at pages/login/login.vue:51", "微信登录成功:", loginRes);
            const authRes = await common_api_index.authApi.wechatLogin({
              code: loginRes.code
            });
            userStore.setToken(authRes.data.token);
            if (authRes.data.refreshToken) {
              userStore.setRefreshToken(authRes.data.refreshToken);
            }
            const userRes = await common_api_index.userApi.getMe();
            userStore.setUserInfo(userRes.data);
            common_vendor.index.showToast({
              title: "登录成功",
              icon: "success"
            });
            setTimeout(() => {
              common_vendor.index.switchTab({
                url: "/pages/index/index"
              });
            }, 1500);
          },
          fail: (err) => {
            common_vendor.index.__f__("error", "at pages/login/login.vue:78", "微信登录失败:", err);
            common_vendor.index.showToast({
              title: "微信登录失败，请重试",
              icon: "none"
            });
          },
          complete: () => {
            loading.value = false;
          }
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/login/login.vue:89", "登录失败:", error);
        common_vendor.index.showToast({
          title: "登录失败，请重试",
          icon: "none"
        });
        loading.value = false;
      }
    };
    const goToRegister = () => {
      common_vendor.index.navigateTo({
        url: "/pages/register/register"
      });
    };
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0$1,
        b: common_vendor.o(handleWechatLogin),
        c: common_vendor.o(goToRegister),
        d: common_vendor.p({
          visible: loading.value,
          text: "登录中..."
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e4e4508d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/login/login.js.map
