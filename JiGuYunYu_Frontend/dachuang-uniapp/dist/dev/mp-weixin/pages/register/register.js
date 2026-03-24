"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_user = require("../../stores/user.js");
const common_api_index = require("../../common/api/index.js");
const common_utils_index = require("../../common/utils/index.js");
if (!Math) {
  Loading();
}
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "register",
  setup(__props) {
    const userStore = stores_user.useUserStore();
    const formData = common_vendor.ref({
      username: "",
      password: "",
      confirmPassword: ""
    });
    const loading = common_vendor.ref(false);
    const canSubmit = common_vendor.computed(() => {
      return formData.value.username.trim().length >= 3 && common_utils_index.validatePassword(formData.value.password) && formData.value.password === formData.value.confirmPassword;
    });
    const handleRegister = async () => {
      if (!canSubmit.value) {
        common_vendor.index.showToast({
          title: "请检查输入信息",
          icon: "none"
        });
        return;
      }
      loading.value = true;
      try {
        await common_api_index.authApi.register({
          username: formData.value.username,
          password: formData.value.password
        });
        const loginRes = await common_api_index.authApi.login({
          username: formData.value.username,
          password: formData.value.password
        });
        userStore.setToken(loginRes.data.token);
        const userRes = await common_api_index.userApi.getMe();
        userStore.setUserInfo(userRes.data);
        common_vendor.index.showToast({
          title: "注册成功",
          icon: "success"
        });
        setTimeout(() => {
          common_vendor.index.switchTab({
            url: "/pages/index/index"
          });
        }, 1500);
      } catch (error) {
        console.error("注册失败:", error);
      } finally {
        loading.value = false;
      }
    };
    const goToLogin = () => {
      common_vendor.index.navigateBack();
    };
    return (_ctx, _cache) => {
      return {
        a: formData.value.username,
        b: common_vendor.o(($event) => formData.value.username = $event.detail.value, "de"),
        c: formData.value.password,
        d: common_vendor.o(($event) => formData.value.password = $event.detail.value, "b3"),
        e: formData.value.confirmPassword,
        f: common_vendor.o(($event) => formData.value.confirmPassword = $event.detail.value, "eb"),
        g: common_vendor.t(loading.value ? "注册中..." : "注册"),
        h: !canSubmit.value ? 1 : "",
        i: common_vendor.o(handleRegister, "a7"),
        j: common_vendor.o(goToLogin, "57"),
        k: common_vendor.p({
          visible: loading.value,
          text: "注册中..."
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-97bb96ad"]]);
wx.createPage(MiniProgramPage);
