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
    const route = common_vendor.useRoute();
    const taskId = route.query.taskId;
    const loading = common_vendor.ref(false);
    const artifact = common_vendor.ref(null);
    common_vendor.onMounted(() => {
      loadResult();
    });
    const loadResult = async () => {
      if (!taskId)
        return;
      loading.value = true;
      try {
        const res = await common_api_index.detectApi.getTaskResult(taskId);
        artifact.value = res.data;
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/result/result.vue:101", "加载结果失败:", error);
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
        d: common_vendor.o(previewImage),
        e: common_vendor.t(artifact.value.confidence),
        f: common_vendor.t(artifact.value.name),
        g: common_vendor.f(artifact.value.tags, (tag, index, i0) => {
          return {
            a: common_vendor.t(tag),
            b: index
          };
        }),
        h: common_vendor.t(artifact.value.period || "未知"),
        i: common_vendor.t(artifact.value.material || "未知"),
        j: common_vendor.t(artifact.value.size || "未知"),
        k: common_vendor.t(artifact.value.location || "未知"),
        l: artifact.value.description
      }, artifact.value.description ? {
        m: common_vendor.t(artifact.value.description)
      } : {}, {
        n: artifact.value.historicalSignificance
      }, artifact.value.historicalSignificance ? {
        o: common_vendor.t(artifact.value.historicalSignificance)
      } : {}) : {
        p: common_vendor.p({
          image: "/static/images/empty-result.png",
          text: "暂无识别结果"
        })
      }, {
        q: artifact.value
      }, artifact.value ? {
        r: common_vendor.o(handleAskAI),
        s: common_vendor.o(handleShare)
      } : {}, {
        t: common_vendor.p({
          visible: loading.value,
          text: "加载中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b615976f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/result/result.js.map
