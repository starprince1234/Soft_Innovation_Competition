"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_user = require("../../stores/user.js");
const common_utils_image = require("../../common/utils/image.js");
const common_api_index = require("../../common/api/index.js");
const common_utils_async = require("../../common/utils/async.js");
const common_utils_index = require("../../common/utils/index.js");
if (!Math) {
  ScanButton();
}
const ScanButton = () => "../../components/ScanButton/ScanButton.js";
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const userStore = stores_user.useUserStore();
    const userInfo = common_vendor.computed(() => userStore.userInfo);
    const banners = common_vendor.ref([
      {
        image: "/static/images/banner1.png",
        title: "探索中华文明",
        desc: "AI智能识别，让文物活起来"
      },
      {
        image: "/static/images/banner2.png",
        title: "传承历史记忆",
        desc: "发现身边的文化瑰宝"
      }
    ]);
    const recentArtifacts = common_vendor.ref([]);
    common_vendor.onMounted(() => {
      loadRecentArtifacts();
    });
    const loadRecentArtifacts = () => {
      const history = common_vendor.index.getStorageSync("detectHistory") || [];
      recentArtifacts.value = history.slice(0, 5);
    };
    const handleScan = async () => {
      try {
        const filePaths = await common_utils_image.chooseImage(1);
        const filePath = filePaths[0];
        common_vendor.index.showLoading({ title: "处理图片中..." });
        const processed = await common_utils_image.processImageForUpload(filePath);
        common_vendor.index.showLoading({ title: "上传中..." });
        const taskRes = await common_api_index.detectApi.createTask({
          imageBase64: processed.base64
        });
        const taskId = taskRes.data.taskId;
        common_vendor.index.showLoading({ title: "识别中..." });
        const result = await common_utils_async.pollTaskStatus(
          taskId,
          common_api_index.detectApi.getTaskStatus,
          common_api_index.detectApi.getTaskResult
        );
        common_vendor.index.hideLoading();
        saveToHistory(result.data);
        common_vendor.index.navigateTo({
          url: `/pages/result/result?taskId=${taskId}`
        });
      } catch (error) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at pages/index/index.vue:162", "识别失败:", error);
        common_vendor.index.showToast({
          title: error.message || "识别失败，请重试",
          icon: "none"
        });
      }
    };
    const saveToHistory = (artifact) => {
      const history = common_vendor.index.getStorageSync("detectHistory") || [];
      history.unshift(artifact);
      common_vendor.index.setStorageSync("detectHistory", history.slice(0, 20));
      loadRecentArtifacts();
    };
    const goToProfile = () => {
      common_vendor.index.switchTab({
        url: "/pages/profile/profile"
      });
    };
    const goToFeedback = () => {
      common_vendor.index.navigateTo({
        url: "/pages/feedback/feedback"
      });
    };
    const goToArtifacts = () => {
      common_vendor.index.switchTab({
        url: "/pages/artifacts/artifacts"
      });
    };
    const goToChat = () => {
      common_vendor.index.switchTab({
        url: "/pages/chat/chat"
      });
    };
    const goToHistory = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const goToAbout = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const goToDetail = (id) => {
      common_vendor.index.navigateTo({
        url: `/pages/artifact-detail/artifact-detail?id=${id}`
      });
    };
    return (_ctx, _cache) => {
      var _a, _b, _c, _d;
      return common_vendor.e({
        a: ((_a = userInfo.value) == null ? void 0 : _a.avatar) || "/static/images/default-avatar.png",
        b: common_vendor.t(((_b = userInfo.value) == null ? void 0 : _b.username) || "游客"),
        c: common_vendor.t(common_vendor.unref(common_utils_index.getRoleName)((_d = (_c = userInfo.value) == null ? void 0 : _c.roles) == null ? void 0 : _d[0])),
        d: common_vendor.o(goToProfile),
        e: common_vendor.o(goToFeedback),
        f: common_vendor.f(banners.value, (item, index, i0) => {
          return {
            a: item.image,
            b: common_vendor.t(item.title),
            c: common_vendor.t(item.desc),
            d: index
          };
        }),
        g: common_vendor.o(goToArtifacts),
        h: common_vendor.o(goToChat),
        i: common_vendor.o(goToHistory),
        j: common_vendor.o(goToAbout),
        k: common_vendor.o(handleScan),
        l: common_vendor.p({
          text: "点击拍照识别"
        }),
        m: recentArtifacts.value.length > 0
      }, recentArtifacts.value.length > 0 ? {
        n: common_vendor.o(goToHistory),
        o: common_vendor.f(recentArtifacts.value, (item, index, i0) => {
          return {
            a: item.image,
            b: common_vendor.t(item.name),
            c: index,
            d: common_vendor.o(($event) => goToDetail(item.id), index)
          };
        })
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
