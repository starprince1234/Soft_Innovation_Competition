"use strict";
const common_vendor = require("../../common/vendor.js");
const common_api_index = require("../../common/api/index.js");
const common_utils_index = require("../../common/utils/index.js");
const common_utils_image = require("../../common/utils/image.js");
if (!Math) {
  (CustomNavbar + Loading)();
}
const CustomNavbar = () => "../../components/CustomNavbar/CustomNavbar.js";
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "feedback",
  setup(__props) {
    const formData = common_vendor.ref({
      type: "BUG",
      textContent: "",
      rating: 5,
      images: [],
      contact: ""
    });
    const loading = common_vendor.ref(false);
    const feedbackTypes = [
      { label: "功能异常", value: "BUG", iconText: "故障" },
      { label: "功能建议", value: "FEATURE_REQUEST", iconText: "建议" },
      { label: "内容错误", value: "CONTENT_ERROR", iconText: "错误" },
      { label: "其他", value: "GENERAL", iconText: "其他" }
    ];
    const canSubmit = common_vendor.computed(() => {
      return formData.value.textContent.trim().length > 0 && formData.value.rating > 0;
    });
    const handleChooseImage = async () => {
      try {
        const count = 3 - formData.value.images.length;
        const filePaths = await common_utils_image.chooseImage(count);
        for (const filePath of filePaths) {
          const processed = await common_utils_image.processImageForUpload(filePath);
          formData.value.images.push({
            url: filePath,
            base64: processed.base64
          });
        }
      } catch (error) {
        console.error("选择图片失败:", error);
        common_vendor.index.showToast({
          title: "选择图片失败",
          icon: "none"
        });
      }
    };
    const deleteImage = (index) => {
      formData.value.images.splice(index, 1);
    };
    const previewImage = (index) => {
      const urls = formData.value.images.map((item) => item.url);
      common_vendor.index.previewImage({
        urls,
        current: index
      });
    };
    const handleSubmit = async () => {
      if (!canSubmit.value) {
        common_vendor.index.showToast({
          title: "请填写反馈内容",
          icon: "none"
        });
        return;
      }
      if (formData.value.contact && !common_utils_index.validatePhone(formData.value.contact) && !common_utils_index.validateEmail(formData.value.contact)) {
        common_vendor.index.showToast({
          title: "请输入正确的手机号或邮箱",
          icon: "none"
        });
        return;
      }
      loading.value = true;
      try {
        const screenshotBase64 = formData.value.images.length > 0 ? formData.value.images[0].base64 : "";
        await common_api_index.feedbackApi.submit({
          type: formData.value.type,
          textContent: formData.value.textContent,
          rating: formData.value.rating,
          screenshotBase64
        });
        common_vendor.index.showToast({
          title: "提交成功",
          icon: "success"
        });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
      } catch (error) {
        console.error("提交反馈失败:", error);
        common_vendor.index.showToast({
          title: error.message || "提交失败，请重试",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.p({
          title: "意见反馈",
          ["show-back"]: true
        }),
        b: common_vendor.f(feedbackTypes, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.iconText),
            b: common_vendor.t(item.label),
            c: formData.value.type === item.value ? 1 : "",
            d: item.value,
            e: common_vendor.o(($event) => formData.value.type = item.value, item.value)
          };
        }),
        c: formData.value.textContent,
        d: common_vendor.o(($event) => formData.value.textContent = $event.detail.value, "d3"),
        e: common_vendor.t(formData.value.textContent.length),
        f: common_vendor.f(5, (i, k0, i0) => {
          return {
            a: i,
            b: i <= formData.value.rating ? 1 : "",
            c: common_vendor.o(($event) => formData.value.rating = i, i)
          };
        }),
        g: common_vendor.f(formData.value.images, (image, index, i0) => {
          return {
            a: image.url,
            b: common_vendor.o(($event) => previewImage(index), index),
            c: common_vendor.o(($event) => deleteImage(index), index),
            d: index
          };
        }),
        h: formData.value.images.length < 3
      }, formData.value.images.length < 3 ? {
        i: common_vendor.o(handleChooseImage, "8b")
      } : {}, {
        j: formData.value.contact,
        k: common_vendor.o(($event) => formData.value.contact = $event.detail.value, "38"),
        l: common_vendor.t(loading.value ? "提交中..." : "提交反馈"),
        m: !canSubmit.value ? 1 : "",
        n: common_vendor.o(handleSubmit, "38"),
        o: common_vendor.p({
          visible: loading.value,
          text: "提交中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-6cdbb6ab"]]);
wx.createPage(MiniProgramPage);
