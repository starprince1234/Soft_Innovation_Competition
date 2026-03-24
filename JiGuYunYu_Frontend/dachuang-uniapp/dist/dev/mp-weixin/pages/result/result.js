"use strict";
const common_vendor = require("../../common/vendor.js");
const common_api_index = require("../../common/api/index.js");
if (!Math) {
  (CustomNavbar + EmptyState + Loading)();
}
const CustomNavbar = () => "../../components/CustomNavbar/CustomNavbar.js";
const EmptyState = () => "../../components/EmptyState/EmptyState.js";
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "result",
  setup(__props) {
    const taskId = common_vendor.ref("");
    common_vendor.onLoad((options = {}) => {
      taskId.value = options.taskId || "";
    });
    const loading = common_vendor.ref(false);
    const artifact = common_vendor.ref(null);
    common_vendor.onMounted(() => {
      loadResult();
    });
    const loadResult = async () => {
      var _a;
      if (!taskId.value)
        return;
      loading.value = true;
      try {
        const detectRes = await common_api_index.detectApi.getTaskResult(taskId.value);
        const detectData = detectRes.data;
        const firstMatch = ((_a = detectData == null ? void 0 : detectData.detectedArtifacts) == null ? void 0 : _a[0]) || null;
        let detail = null;
        if (firstMatch == null ? void 0 : firstMatch.artifactId) {
          const detailRes = await common_api_index.artifactsApi.getDetail(firstMatch.artifactId);
          detail = detailRes.data;
        }
        artifact.value = {
          id: (detail == null ? void 0 : detail.id) || (firstMatch == null ? void 0 : firstMatch.artifactId) || (detectData == null ? void 0 : detectData.taskId),
          name: (detail == null ? void 0 : detail.name) || (firstMatch == null ? void 0 : firstMatch.label) || "识别结果",
          imageUrl: (detail == null ? void 0 : detail.imageUrl) || (detectData == null ? void 0 : detectData.imageUrl) || "/static/images/empty-result.png",
          confidence: typeof (firstMatch == null ? void 0 : firstMatch.confidence) === "number" ? Math.round(firstMatch.confidence * 100) : null,
          tags: (detail == null ? void 0 : detail.tags) || [],
          period: detail == null ? void 0 : detail.era,
          material: detail == null ? void 0 : detail.category,
          size: null,
          location: detail == null ? void 0 : detail.location,
          description: detail == null ? void 0 : detail.description
        };
      } catch (error) {
        console.error("加载结果失败:", error);
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    };
    const previewImage = () => {
      var _a;
      if ((_a = artifact.value) == null ? void 0 : _a.imageUrl) {
        common_vendor.index.previewImage({
          urls: [artifact.value.imageUrl]
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
          title: "识别结果",
          ["show-back"]: true
        }),
        b: artifact.value
      }, artifact.value ? common_vendor.e({
        c: artifact.value.imageUrl,
        d: common_vendor.o(previewImage, "e2"),
        e: artifact.value.confidence !== null
      }, artifact.value.confidence !== null ? {
        f: common_vendor.t(artifact.value.confidence)
      } : {}, {
        g: common_vendor.t(artifact.value.name),
        h: common_vendor.f(artifact.value.tags, (tag, index, i0) => {
          return {
            a: common_vendor.t(tag),
            b: index
          };
        }),
        i: common_vendor.t(artifact.value.period || "未知"),
        j: common_vendor.t(artifact.value.material || "未知"),
        k: common_vendor.t(artifact.value.size || "未知"),
        l: common_vendor.t(artifact.value.location || "未知"),
        m: artifact.value.description
      }, artifact.value.description ? {
        n: common_vendor.t(artifact.value.description)
      } : {}) : {
        o: common_vendor.p({
          image: "/static/images/empty-result.png",
          text: "暂无识别结果"
        })
      }, {
        p: artifact.value
      }, artifact.value ? {
        q: common_vendor.o(handleAskAI, "7c"),
        r: common_vendor.o(handleShare, "4c")
      } : {}, {
        s: common_vendor.p({
          visible: loading.value,
          text: "加载中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d38065ce"]]);
wx.createPage(MiniProgramPage);
