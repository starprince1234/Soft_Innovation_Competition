"use strict";
const common_vendor = require("../common/vendor.js");
const common_constants_index = require("../common/constants/index.js");
const useChatStore = common_vendor.defineStore("chat", {
  state: () => ({
    currentDialogId: null,
    chatHistory: [],
    isLoading: false
  }),
  getters: {
    hasHistory: (state) => state.chatHistory.length > 0
  },
  actions: {
    addMessage(message) {
      this.chatHistory.push(message);
      this.saveToStorage();
    },
    updateLastMessage(content) {
      if (this.chatHistory.length > 0) {
        const lastMessage = this.chatHistory[this.chatHistory.length - 1];
        if (lastMessage.type === "ai") {
          lastMessage.content = content;
          this.saveToStorage();
        }
      }
    },
    clearHistory() {
      this.chatHistory = [];
      this.currentDialogId = null;
      this.saveToStorage();
    },
    setCurrentDialogId(dialogId) {
      this.currentDialogId = dialogId;
    },
    setLoading(loading) {
      this.isLoading = loading;
    },
    saveToStorage() {
      common_vendor.index.setStorageSync(common_constants_index.STORAGE_KEYS.CHAT_HISTORY, this.chatHistory);
    },
    loadFromStorage() {
      const history = common_vendor.index.getStorageSync(common_constants_index.STORAGE_KEYS.CHAT_HISTORY);
      if (history) {
        this.chatHistory = history;
      }
    }
  }
});
exports.useChatStore = useChatStore;
//# sourceMappingURL=../../.sourcemap/mp-weixin/stores/chat.js.map
