"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "EmptyState",
  props: {
    image: {
      type: String,
      default: "/static/images/empty.png"
    },
    text: {
      type: String,
      default: "暂无数据"
    },
    showAction: {
      type: Boolean,
      default: false
    }
  },
  setup(__props) {
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.image,
        b: common_vendor.t(__props.text),
        c: __props.showAction
      }, __props.showAction ? {} : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-00c0c74e"]]);
wx.createComponent(Component);
