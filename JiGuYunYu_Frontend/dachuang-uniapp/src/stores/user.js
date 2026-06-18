import { defineStore } from 'pinia'
import { STORAGE_KEYS } from '@/common/constants/index.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.username || '',
    userRole: (state) => state.userInfo?.role || '',
    userId: (state) => state.userInfo?.id || ''
  },

  actions: {
    setToken(token) {
      this.token = token
      uni.setStorageSync(STORAGE_KEYS.TOKEN, token)
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo
      uni.setStorageSync(STORAGE_KEYS.USER_INFO, userInfo)
    },

    logout() {
      this.token = ''
      this.userInfo = null
      uni.removeStorageSync(STORAGE_KEYS.TOKEN)
      uni.removeStorageSync(STORAGE_KEYS.USER_INFO)
      uni.reLaunch({
        url: '/pages/login/login'
      })
    },

    initFromStorage() {
      const token = uni.getStorageSync(STORAGE_KEYS.TOKEN)
      const userInfo = uni.getStorageSync(STORAGE_KEYS.USER_INFO)

      if (token) this.token = token
      if (userInfo) this.userInfo = userInfo
    }
  }
})