"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const stores_user = require("../../stores/user.js");
const stores_chat = require("../../stores/chat.js");
const common_api_index = require("../../common/api/index.js");
const common_utils_async = require("../../common/utils/async.js");
const common_utils_index = require("../../common/utils/index.js");
if (!Math) {
  (EmptyState + Loading)();
}
const EmptyState = () => "../../components/EmptyState/EmptyState.js";
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "chat",
  setup(__props) {
    const route = common_vendor.useRoute();
    const userStore = stores_user.useUserStore();
    const chatStore = stores_chat.useChatStore();
    const userInfo = common_vendor.computed(() => userStore.userInfo);
    const chatHistory = common_vendor.computed(() => chatStore.chatHistory);
    const isLoading = common_vendor.computed(() => chatStore.isLoading);
    const inputMessage = common_vendor.ref("");
    const scrollToView = common_vendor.ref("");
    const artifactId = common_vendor.ref(route.query.artifactId || "");
    const artifactName = common_vendor.ref(route.query.artifactName || "");
    const quickQuestions = common_vendor.ref([
      "这件文物有什么历史意义？",
      "这件文物的制作工艺是怎样的？",
      "这件文物属于哪个朝代？",
      "这件文物的特点是什么？"
    ]);
    common_vendor.onMounted(() => {
      chatStore.loadFromStorage();
      if (artifactName.value) {
        inputMessage.value = `关于"${artifactName.value}"，我想了解更多`;
      }
    });
    common_vendor.watch(chatHistory, () => {
      scrollToBottom();
    }, { deep: true });
    const canSend = common_vendor.computed(() => {
      return inputMessage.value.trim().length > 0 && !isLoading.value;
    });
    const handleSend = async () => {
      if (!canSend.value)
        return;
      const message = inputMessage.value.trim();
      inputMessage.value = "";
      chatStore.addMessage({
        type: "user",
        content: message,
        timestamp: Date.now()
      });
      chatStore.setLoading(true);
      try {
        const res = await common_api_index.dialogApi.createRequest({
          query: message,
          artifactId: artifactId.value,
          contextHistory: chatHistory.value.map((msg) => ({
            role: msg.type === "user" ? "user" : "assistant",
            content: msg.content
          }))
        });
        const dialogTaskId = res.data.dialogTaskId;
        chatStore.setCurrentDialogId(dialogTaskId);
        const result = await common_utils_async.pollTaskStatus(
          dialogTaskId,
          async (id) => {
            const statusRes = await common_api_index.dialogApi.getResult(id);
            return statusRes.data.status;
          },
          async (id) => {
            return await common_api_index.dialogApi.getResult(id);
          }
        );
        chatStore.addMessage({
          type: "ai",
          content: result.data.aiResponse,
          timestamp: Date.now()
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/chat/chat.vue:191", "发送消息失败:", error);
        common_vendor.index.showToast({
          title: error.message || "发送失败，请重试",
          icon: "none"
        });
      } finally {
        chatStore.setLoading(false);
      }
    };
    const handleQuickQuestion = (question) => {
      inputMessage.value = question;
      handleSend();
    };
    const handleClearHistory = async () => {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要清空对话历史吗？",
        success: async (res) => {
          if (res.confirm) {
            try {
              await common_api_index.dialogApi.clearHistories(artifactId.value);
              chatStore.clearHistory();
              common_vendor.index.showToast({
                title: "已清空",
                icon: "success"
              });
            } catch (error) {
              common_vendor.index.__f__("error", "at pages/chat/chat.vue:220", "清空历史失败:", error);
              chatStore.clearHistory();
              common_vendor.index.showToast({
                title: "已清空",
                icon: "success"
              });
            }
          }
        }
      });
    };
    const scrollToBottom = () => {
      common_vendor.nextTick$1(() => {
        if (chatHistory.value.length > 0) {
          scrollToView.value = "msg-" + (chatHistory.value.length - 1);
        }
      });
    };
    const formatTime = (timestamp) => {
      return common_utils_index.formatDate(timestamp, "HH:mm");
    };
    const formatMessage = (content) => {
      return common_utils_index.parseMarkdown(content);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(handleClearHistory),
        b: common_vendor.f(chatHistory.value, (msg, index, i0) => {
          var _a;
          return common_vendor.e({
            a: msg.type === "ai"
          }, msg.type === "ai" ? {
            b: common_assets._imports_0$2
          } : {}, {
            c: msg.type === "user"
          }, msg.type === "user" ? {
            d: common_vendor.t(msg.content)
          } : {
            e: formatMessage(msg.content)
          }, {
            f: common_vendor.t(formatTime(msg.timestamp)),
            g: msg.type === "user"
          }, msg.type === "user" ? {
            h: ((_a = userInfo.value) == null ? void 0 : _a.avatar) || "/static/images/default-avatar.png"
          } : {}, {
            i: "msg-" + index,
            j: common_vendor.n(msg.type),
            k: index
          });
        }),
        c: isLoading.value
      }, isLoading.value ? {
        d: common_assets._imports_0$2
      } : {}, {
        e: chatHistory.value.length === 0 && !isLoading.value
      }, chatHistory.value.length === 0 && !isLoading.value ? {
        f: common_vendor.f(quickQuestions.value, (question, index, i0) => {
          return {
            a: common_vendor.t(question),
            b: index,
            c: common_vendor.o(($event) => handleQuickQuestion(question), index)
          };
        }),
        g: common_vendor.p({
          image: "/static/images/empty-chat.png",
          text: "开始与AI对话吧",
          ["show-action"]: true
        })
      } : {}, {
        h: scrollToView.value,
        i: isLoading.value,
        j: common_vendor.o(handleSend),
        k: inputMessage.value,
        l: common_vendor.o(($event) => inputMessage.value = $event.detail.value),
        m: !canSend.value ? 1 : "",
        n: common_vendor.o(handleSend),
        o: common_vendor.p({
          visible: false
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0a633310"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/chat/chat.js.map
