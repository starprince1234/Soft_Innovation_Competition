"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "favorites",
  setup(__props) {
    const favoritesList = common_vendor.ref([]);
    common_vendor.onMounted(() => {
      loadFavorites();
    });
    const loadFavorites = () => {
      const favorites = common_vendor.index.getStorageSync("favorites") || [];
      favoritesList.value = favorites;
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
    const unfavoriteItem = (index) => {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要取消收藏吗？",
        success: (res) => {
          if (res.confirm) {
            favoritesList.value.splice(index, 1);
            common_vendor.index.setStorageSync("favorites", favoritesList.value);
            common_vendor.index.showToast({
              title: "取消收藏成功",
              icon: "success"
            });
          }
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: favoritesList.value.length > 0
      }, favoritesList.value.length > 0 ? {
        b: common_vendor.f(favoritesList.value, (item, index, i0) => {
          return {
            a: item.image,
            b: common_vendor.t(item.name || "未命名文物"),
            c: common_vendor.o(($event) => goToDetail(item.id), index),
            d: common_vendor.o(($event) => unfavoriteItem(index), index),
            e: index
          };
        })
      } : {
        c: common_assets._imports_0
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-da3e0273"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/favorites/favorites.js.map
