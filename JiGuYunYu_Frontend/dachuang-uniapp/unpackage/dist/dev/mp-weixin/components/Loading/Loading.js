"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "Loading",
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    text: {
      type: String,
      default: "加载中..."
    },
    maskClosable: {
      type: Boolean,
      default: false
    }
  },
  emits: ["close"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const handleMaskClick = () => {
      if (props.maskClosable) {
        emit("close");
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? {
        b: common_vendor.o(handleMaskClick),
        c: common_vendor.f(3, (i, k0, i0) => {
          return {
            a: i
          };
        }),
        d: common_vendor.t(__props.text)
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-fbde6292"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/components/Loading/Loading.js.map
