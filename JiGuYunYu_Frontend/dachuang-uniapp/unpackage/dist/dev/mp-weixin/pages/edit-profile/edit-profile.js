"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_user = require("../../stores/user.js");
const _sfc_main = {
  __name: "edit-profile",
  setup(__props) {
    const userStore = stores_user.useUserStore();
    const formData = common_vendor.ref({
      avatar: "",
      username: "",
      nickname: "",
      phone: "",
      email: "",
      bio: ""
    });
    common_vendor.onMounted(() => {
      loadUserInfo();
    });
    const loadUserInfo = () => {
      const userInfo = userStore.userInfo;
      if (userInfo) {
        formData.value = {
          avatar: userInfo.avatar || "",
          username: userInfo.username || "",
          nickname: userInfo.nickname || "",
          phone: userInfo.phone || "",
          email: userInfo.email || "",
          bio: userInfo.bio || ""
        };
      }
    };
    const handleAvatarUpload = () => {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: (res) => {
          const tempFilePath = res.tempFilePaths[0];
          formData.value.avatar = tempFilePath;
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at pages/edit-profile/edit-profile.vue:92", "选择图片失败:", err);
          common_vendor.index.showToast({
            title: "选择图片失败",
            icon: "none"
          });
        }
      });
    };
    const handleSave = () => {
      if (!formData.value.username) {
        common_vendor.index.showToast({
          title: "请输入用户名",
          icon: "none"
        });
        return;
      }
      common_vendor.index.showLoading({ title: "保存中..." });
      setTimeout(() => {
        common_vendor.index.hideLoading();
        const updatedUserInfo = {
          ...userStore.userInfo,
          ...formData.value
        };
        userStore.setUserInfo(updatedUserInfo);
        common_vendor.index.showToast({
          title: "保存成功",
          icon: "success"
        });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
      }, 1e3);
    };
    return (_ctx, _cache) => {
      return {
        a: formData.value.avatar || "/static/images/default-avatar.png",
        b: common_vendor.o(handleAvatarUpload),
        c: formData.value.username,
        d: common_vendor.o(($event) => formData.value.username = $event.detail.value),
        e: formData.value.nickname,
        f: common_vendor.o(($event) => formData.value.nickname = $event.detail.value),
        g: formData.value.phone,
        h: common_vendor.o(($event) => formData.value.phone = $event.detail.value),
        i: formData.value.email,
        j: common_vendor.o(($event) => formData.value.email = $event.detail.value),
        k: formData.value.bio,
        l: common_vendor.o(($event) => formData.value.bio = $event.detail.value),
        m: common_vendor.o(handleSave)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c0f45e44"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/edit-profile/edit-profile.js.map
