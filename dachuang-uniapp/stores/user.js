import { defineStore } from 'pinia'
import { STORAGE_KEYS } from '@/common/constants/index.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    refreshToken: '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.username || '',
    userRole: (state) => state.userInfo?.roles?.[0] || '',
    userId: (state) => state.userInfo?.id || ''
  },

  actions: {
    setToken(token) {
      this.token = token
      uni.setStorageSync(STORAGE_KEYS.TOKEN, token)
    },

    setRefreshToken(refreshToken) {
      this.refreshToken = refreshToken
      uni.setStorageSync(STORAGE_KEYS.REFRESH_TOKEN, refreshToken)
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo
      uni.setStorageSync(STORAGE_KEYS.USER_INFO, userInfo)
    },

    logout() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = null
      uni.removeStorageSync(STORAGE_KEYS.TOKEN)
      uni.removeStorageSync(STORAGE_KEYS.REFRESH_TOKEN)
      uni.removeStorageSync(STORAGE_KEYS.USER_INFO)
      uni.reLaunch({
        url: '/pages/login/login'
      })
    },

    initFromStorage() {
      const token = uni.getStorageSync(STORAGE_KEYS.TOKEN)
      const refreshToken = uni.getStorageSync(STORAGE_KEYS.REFRESH_TOKEN)
      const userInfo = uni.getStorageSync(STORAGE_KEYS.USER_INFO)

      if (token) this.token = token
      if (refreshToken) this.refreshToken = refreshToken
      if (userInfo) this.userInfo = userInfo
    }
  }
})