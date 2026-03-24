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
    const formData = common_vendor.ref({
      username: "",
      password: ""
    });
    const canSubmit = common_vendor.computed(() => {
      return formData.value.username.trim().length > 0 && formData.value.password.length >= 6;
    });
    const handleLogin = async () => {
      if (!canSubmit.value) {
        common_vendor.index.showToast({
          title: "请输入用户名和密码",
          icon: "none"
        });
        return;
      }
      loading.value = true;
      try {
        const authRes = await common_api_index.authApi.login({
          username: formData.value.username,
          password: formData.value.password
        });
        userStore.setToken(authRes.data.token);
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
      } catch (error) {
        console.error("登录失败:", error);
      } finally {
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
        b: formData.value.username,
        c: common_vendor.o(($event) => formData.value.username = $event.detail.value, "ff"),
        d: formData.value.password,
        e: common_vendor.o(($event) => formData.value.password = $event.detail.value, "9e"),
        f: common_vendor.t(loading.value ? "登录中..." : "登录"),
        g: !canSubmit.value ? 1 : "",
        h: common_vendor.o(handleLogin, "77"),
        i: common_vendor.o(goToRegister, "22"),
        j: common_vendor.p({
          visible: loading.value,
          text: "登录中..."
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-cdfe2409"]]);
wx.createPage(MiniProgramPage);
