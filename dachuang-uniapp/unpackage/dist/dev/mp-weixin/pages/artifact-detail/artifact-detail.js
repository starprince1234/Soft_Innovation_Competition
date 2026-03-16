"use strict";
const common_vendor = require("../../common/vendor.js");
const common_api_index = require("../../common/api/index.js");
const common_utils_index = require("../../common/utils/index.js");
if (!Math) {
  (CustomNavbar + EmptyState + Loading)();
}
const CustomNavbar = () => "../../components/CustomNavbar/CustomNavbar.js";
const EmptyState = () => "../../components/EmptyState/EmptyState.js";
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "artifact-detail",
  setup(__props) {
    const route = common_vendor.useRoute();
    const artifactId = route.query.id;
    const loading = common_vendor.ref(false);
    const artifact = common_vendor.ref(null);
    const relatedArtifacts = common_vendor.ref([]);
    common_vendor.onMounted(() => {
      loadDetail();
    });
    const loadDetail = async () => {
      if (!artifactId)
        return;
      loading.value = true;
      try {
        const res = await common_api_index.artifactsApi.getDetail(artifactId);
        artifact.value = res.data;
        if (res.data.relatedArtifacts) {
          relatedArtifacts.value = res.data.relatedArtifacts;
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/artifact-detail/artifact-detail.vue:139", "加载详情失败:", error);
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    };
    const previewImage = (index) => {
      var _a;
      if ((_a = artifact.value) == null ? void 0 : _a.images) {
        common_vendor.index.previewImage({
          urls: artifact.value.images,
          current: index
        });
      }
    };
    const handleAskAI = () => {
      if (!artifact.value)
        return;
      common_vendor.index.navigateTo({
        url: `/pages/chat/chat?artifactId=${artifact.value.id}&artifactName=${encodeURIComponent(artifact.value.name)}`
      });
    };
    const handleShare = () => {
      common_vendor.index.showShareMenu({
        withShareTicket: true
      });
    };
    const goToDetail = (id) => {
      common_vendor.index.redirectTo({
        url: `/pages/artifact-detail/artifact-detail?id=${id}`
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.p({
          title: "文物详情",
          ["show-back"]: true
        }),
        b: artifact.value
      }, artifact.value ? common_vendor.e({
        c: common_vendor.f(artifact.value.images, (image, index, i0) => {
          return {
            a: image,
            b: common_vendor.o(($event) => previewImage(index), index),
            c: index
          };
        }),
        d: common_vendor.t(artifact.value.name),
        e: common_vendor.t(common_vendor.unref(common_utils_index.getStatusText)(artifact.value.status)),
        f: common_vendor.unref(common_utils_index.getStatusColor)(artifact.value.status),
        g: artifact.value.tags && artifact.value.tags.length > 0
      }, artifact.value.tags && artifact.value.tags.length > 0 ? {
        h: common_vendor.f(artifact.value.tags, (tag, index, i0) => {
          return {
            a: common_vendor.t(tag),
            b: index
          };
        })
      } : {}, {
        i: common_vendor.t(artifact.value.period || "未知"),
        j: common_vendor.t(artifact.value.material || "未知"),
        k: common_vendor.t(artifact.value.size || "未知"),
        l: common_vendor.t(artifact.value.weight || "未知"),
        m: common_vendor.t(artifact.value.location || "未知"),
        n: common_vendor.t(artifact.value.museum || "未知"),
        o: artifact.value.description
      }, artifact.value.description ? {
        p: common_vendor.t(artifact.value.description)
      } : {}, {
        q: artifact.value.historicalSignificance
      }, artifact.value.historicalSignificance ? {
        r: common_vendor.t(artifact.value.historicalSignificance)
      } : {}, {
        s: artifact.value.craftsmanship
      }, artifact.value.craftsmanship ? {
        t: common_vendor.t(artifact.value.craftsmanship)
      } : {}, {
        v: relatedArtifacts.value.length > 0
      }, relatedArtifacts.value.length > 0 ? {
        w: common_vendor.f(relatedArtifacts.value, (item, index, i0) => {
          return {
            a: item.imageUrl,
            b: common_vendor.t(item.name),
            c: index,
            d: common_vendor.o(($event) => goToDetail(item.id), index)
          };
        })
      } : {}) : {
        x: common_vendor.p({
          image: "/static/images/empty.png",
          text: "暂无数据"
        })
      }, {
        y: artifact.value
      }, artifact.value ? {
        z: common_vendor.o(handleAskAI),
        A: common_vendor.o(handleShare)
      } : {}, {
        B: common_vendor.p({
          visible: loading.value,
          text: "加载中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-30681d87"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/artifact-detail/artifact-detail.js.map
