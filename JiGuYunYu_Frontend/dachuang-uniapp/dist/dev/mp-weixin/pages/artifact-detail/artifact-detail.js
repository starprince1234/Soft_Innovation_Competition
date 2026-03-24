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
    const artifactId = common_vendor.ref("");
    common_vendor.onLoad((options = {}) => {
      artifactId.value = options.id || "";
    });
    const loading = common_vendor.ref(false);
    const artifact = common_vendor.ref(null);
    const artifactImages = common_vendor.computed(() => {
      if (!artifact.value)
        return [];
      const images = [artifact.value.imageUrl, artifact.value.thumbnailUrl].filter(Boolean);
      return [...new Set(images)];
    });
    common_vendor.onMounted(() => {
      loadDetail();
    });
    const loadDetail = async () => {
      if (!artifactId.value)
        return;
      loading.value = true;
      try {
        const res = await common_api_index.artifactsApi.getDetail(artifactId.value);
        artifact.value = res.data;
      } catch (error) {
        console.error("加载详情失败:", error);
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    };
    const previewImage = (index) => {
      if (artifactImages.value.length > 0) {
        common_vendor.index.previewImage({
          urls: artifactImages.value,
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
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.p({
          title: "文物详情",
          ["show-back"]: true
        }),
        b: artifact.value
      }, artifact.value ? common_vendor.e({
        c: common_vendor.f(artifactImages.value, (image, index, i0) => {
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
        i: common_vendor.t(artifact.value.era || "未知"),
        j: common_vendor.t(artifact.value.category || "未知"),
        k: common_vendor.t(artifact.value.location || "未知"),
        l: artifact.value.description
      }, artifact.value.description ? {
        m: common_vendor.t(artifact.value.description)
      } : {}) : {
        n: common_vendor.p({
          image: "/static/images/empty.png",
          text: "暂无数据"
        })
      }, {
        o: artifact.value
      }, artifact.value ? {
        p: common_vendor.o(handleAskAI, "63"),
        q: common_vendor.o(handleShare, "5b")
      } : {}, {
        r: common_vendor.p({
          visible: loading.value,
          text: "加载中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-65445635"]]);
wx.createPage(MiniProgramPage);
