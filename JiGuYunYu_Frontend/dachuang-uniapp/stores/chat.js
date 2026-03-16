import { defineStore } from 'pinia'
import { STORAGE_KEYS } from '@/common/constants/index.js'

export const useChatStore = defineStore('chat', {
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
      this.chatHistory.push(message)
      this.saveToStorage()
    },

    updateLastMessage(content) {
      if (this.chatHistory.length > 0) {
        const lastMessage = this.chatHistory[this.chatHistory.length - 1]
        if (lastMessage.type === 'ai') {
          lastMessage.content = content
          this.saveToStorage()
        }
      }
    },

    clearHistory() {
      this.chatHistory = []
      this.currentDialogId = null
      this.saveToStorage()
    },

    setCurrentDialogId(dialogId) {
      this.currentDialogId = dialogId
    },

    setLoading(loading) {
      this.isLoading = loading
    },

    saveToStorage() {
      uni.setStorageSync(STORAGE_KEYS.CHAT_HISTORY, this.chatHistory)
    },

    loadFromStorage() {
      const history = uni.getStorageSync(STORAGE_KEYS.CHAT_HISTORY)
      if (history) {
        this.chatHistory = history
      }
    }
  }
})