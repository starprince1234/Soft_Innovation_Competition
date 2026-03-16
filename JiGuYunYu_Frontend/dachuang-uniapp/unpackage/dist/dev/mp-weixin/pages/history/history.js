"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "history",
  setup(__props) {
    const historyList = common_vendor.ref([]);
    common_vendor.onMounted(() => {
      loadHistory();
    });
    const loadHistory = () => {
      const history = common_vendor.index.getStorageSync("detectHistory") || [];
      historyList.value = history;
    };
    const formatTime = (timestamp) => {
      if (!timestamp)
        return "";
      const date = new Date(timestamp);
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")} ${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
    };
    const goToDetail = (id) => {
      if (id) {
        common_vendor.index.navigateTo({
          url: `/pages/artifact-detail/artifact-detail?id=${id}`
        });
      } else {
        common_vendor.index.showToast({
          title: "无法查看详情",
          icon: "none"
        });
      }
    };
    const deleteItem = (index) => {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要删除这条记录吗？",
        success: (res) => {
          if (res.confirm) {
            historyList.value.splice(index, 1);
            common_vendor.index.setStorageSync("detectHistory", historyList.value);
            common_vendor.index.showToast({
              title: "删除成功",
              icon: "success"
            });
          }
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: historyList.value.length > 0
      }, historyList.value.length > 0 ? {
        b: common_vendor.f(historyList.value, (item, index, i0) => {
          return {
            a: common_vendor.t(item.name || "未命名文物"),
            b: common_vendor.t(formatTime(item.timestamp)),
            c: item.image,
            d: common_vendor.t(item.description || "暂无描述"),
            e: common_vendor.o(($event) => goToDetail(item.id), index),
            f: common_vendor.o(($event) => deleteItem(index), index),
            g: index
          };
        })
      } : {
        c: common_assets._imports_0
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b2d018fa"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/history/history.js.map
