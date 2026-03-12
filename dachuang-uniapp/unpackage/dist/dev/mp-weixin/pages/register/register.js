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
      email: "",
      code: "",
      username: "",
      password: "",
      confirmPassword: ""
    });
    const loading = common_vendor.ref(false);
    const countdown = common_vendor.ref(0);
    let countdownTimer = null;
    const canSubmit = common_vendor.computed(() => {
      return common_utils_index.validateEmail(formData.value.email) && formData.value.code.length === 6 && formData.value.username.trim().length > 0 && common_utils_index.validatePassword(formData.value.password) && formData.value.password === formData.value.confirmPassword;
    });
    const handleSendCode = async () => {
      if (!common_utils_index.validateEmail(formData.value.email)) {
        common_vendor.index.showToast({
          title: "请输入正确的QQ邮箱",
          icon: "none"
        });
        return;
      }
      if (countdown.value > 0)
        return;
      try {
        common_vendor.index.showToast({
          title: "验证码已发送",
          icon: "success"
        });
        countdown.value = 60;
        countdownTimer = setInterval(() => {
          countdown.value--;
          if (countdown.value <= 0) {
            clearInterval(countdownTimer);
          }
        }, 1e3);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/register/register.vue:138", "发送验证码失败:", error);
      }
    };
    const handleRegister = async () => {
      if (!canSubmit.value) {
        common_vendor.index.showToast({
          title: "请填写完整信息",
          icon: "none"
        });
        return;
      }
      loading.value = true;
      try {
        const res = await common_api_index.authApi.register({
          email: formData.value.email,
          code: formData.value.code,
          username: formData.value.username,
          password: formData.value.password
        });
        userStore.setToken(res.data.token);
        if (res.data.refreshToken) {
          userStore.setRefreshToken(res.data.refreshToken);
        }
        if (res.data.user) {
          userStore.setUserInfo(res.data.user);
        }
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
        common_vendor.index.__f__("error", "at pages/register/register.vue:180", "注册失败:", error);
      } finally {
        loading.value = false;
      }
    };
    const goToLogin = () => {
      common_vendor.index.navigateBack();
    };
    return (_ctx, _cache) => {
      return {
        a: formData.value.email,
        b: common_vendor.o(($event) => formData.value.email = $event.detail.value),
        c: formData.value.code,
        d: common_vendor.o(($event) => formData.value.code = $event.detail.value),
        e: common_vendor.t(countdown.value > 0 ? `${countdown.value}s` : "获取验证码"),
        f: countdown.value > 0 ? 1 : "",
        g: common_vendor.o(handleSendCode),
        h: formData.value.username,
        i: common_vendor.o(($event) => formData.value.username = $event.detail.value),
        j: formData.value.password,
        k: common_vendor.o(($event) => formData.value.password = $event.detail.value),
        l: formData.value.confirmPassword,
        m: common_vendor.o(($event) => formData.value.confirmPassword = $event.detail.value),
        n: common_vendor.t(loading.value ? "注册中..." : "注册"),
        o: !canSubmit.value ? 1 : "",
        p: common_vendor.o(handleRegister),
        q: common_vendor.o(goToLogin),
        r: common_vendor.p({
          visible: loading.value,
          text: "注册中..."
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-bac4a35d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/register/register.js.map
