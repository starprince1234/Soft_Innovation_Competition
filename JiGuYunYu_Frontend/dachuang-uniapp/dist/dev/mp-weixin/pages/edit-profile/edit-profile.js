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
          console.error("选择图片失败:", err);
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
        b: common_vendor.o(handleAvatarUpload, "f0"),
        c: formData.value.username,
        d: common_vendor.o(($event) => formData.value.username = $event.detail.value, "23"),
        e: formData.value.nickname,
        f: common_vendor.o(($event) => formData.value.nickname = $event.detail.value, "fc"),
        g: formData.value.phone,
        h: common_vendor.o(($event) => formData.value.phone = $event.detail.value, "ca"),
        i: formData.value.email,
        j: common_vendor.o(($event) => formData.value.email = $event.detail.value, "07"),
        k: formData.value.bio,
        l: common_vendor.o(($event) => formData.value.bio = $event.detail.value, "ff"),
        m: common_vendor.o(handleSave, "82")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-043f977d"]]);
wx.createPage(MiniProgramPage);
