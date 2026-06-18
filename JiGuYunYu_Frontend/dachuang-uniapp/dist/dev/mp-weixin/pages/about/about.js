"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "about",
  setup(__props) {
    const openLink = (url) => {
      common_vendor.index.openURL({
        url,
        success: () => {
          console.log("打开链接成功");
        },
        fail: (err) => {
          console.error("打开链接失败:", err);
          common_vendor.index.showToast({
            title: "打开链接失败",
            icon: "none"
          });
        }
      });
    };
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0$1,
        b: common_vendor.o(($event) => openLink("https://www.jiguyunyu.com"), "43"),
        c: common_vendor.o(($event) => openLink("https://www.jiguyunyu.com/privacy"), "fa"),
        d: common_vendor.o(($event) => openLink("https://www.jiguyunyu.com/terms"), "82")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b5177f87"]]);
wx.createPage(MiniProgramPage);
