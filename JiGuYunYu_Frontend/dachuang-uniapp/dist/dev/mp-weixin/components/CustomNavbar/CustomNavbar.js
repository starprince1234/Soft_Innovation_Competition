"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "CustomNavbar",
  props: {
    title: {
      type: String,
      default: ""
    },
    showBack: {
      type: Boolean,
      default: true
    },
    bgColor: {
      type: String,
      default: "#FFFFFF"
    }
  },
  emits: ["back"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    const statusBarHeight = common_vendor.ref(0);
    const navbarHeight = common_vendor.ref(44);
    common_vendor.onMounted(() => {
      const systemInfo = common_vendor.index.getSystemInfoSync();
      statusBarHeight.value = systemInfo.statusBarHeight || 0;
    });
    const handleBack = () => {
      emit("back");
      common_vendor.index.navigateBack({
        delta: 1
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.showBack
      }, __props.showBack ? {} : {}, {
        b: common_vendor.o(handleBack, "52"),
        c: common_vendor.t(__props.title),
        d: navbarHeight.value + "px",
        e: statusBarHeight.value + "px",
        f: __props.bgColor
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f3889534"]]);
wx.createComponent(Component);
