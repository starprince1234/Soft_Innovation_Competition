"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "ScanButton",
  props: {
    text: {
      type: String,
      default: "点击拍照识别"
    }
  },
  emits: "scan",
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    const handleScan = () => {
      emit("scan");
    };
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(__props.text),
        b: common_vendor.o(handleScan)
      };
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-a82ecea8"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/components/ScanButton/ScanButton.js.map
