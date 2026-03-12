import { API_BASE_URL, STORAGE_KEYS } from '@/common/constants/index.js'

class Request {
  constructor() {
    this.baseURL = API_BASE_URL
    this.timeout = 30000
  }

  getHeader() {
    const token = uni.getStorageSync(STORAGE_KEYS.TOKEN)
    return {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    }
  }

  request(options) {
    return new Promise((resolve, reject) => {
      uni.request({
        url: this.baseURL + options.url,
        method: options.method || 'GET',
        data: options.data || {},
        header: {
          ...this.getHeader(),
          ...options.header
        },
        timeout: this.timeout,
        success: (res) => {
          const { statusCode, data } = res
          if (statusCode === 200) {
            if (data.code === 200) {
              resolve(data)
            } else {
              this.handleError(data)
              reject(data)
            }
          } else if (statusCode === 401) {
            this.handleUnauthorized()
            reject({ message: '登录已过期，请重新登录' })
          } else {
            reject({ message: `请求失败，状态码：${statusCode}` })
          }
        },
        fail: (err) => {
          console.error('请求失败:', err)
          uni.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(err)
        }
      })
    })
  }

  get(url, params = {}) {
    return this.request({
      url,
      method: 'GET',
      data: params
    })
  }

  post(url, data = {}) {
    return this.request({
      url,
      method: 'POST',
      data
    })
  }

  put(url, data = {}) {
    return this.request({
      url,
      method: 'PUT',
      data
    })
  }

  delete(url, data = {}) {
    return this.request({
      url,
      method: 'DELETE',
      data
    })
  }

  handleError(data) {
    const message = data.message || '请求失败'
    uni.showToast({
      title: message,
      icon: 'none',
      duration: 2000
    })
  }

  handleUnauthorized() {
    uni.removeStorageSync(STORAGE_KEYS.TOKEN)
    uni.removeStorageSync(STORAGE_KEYS.REFRESH_TOKEN)
    uni.removeStorageSync(STORAGE_KEYS.USER_INFO)
    uni.reLaunch({
      url: '/pages/login/login'
    })
  }
}

const request = new Request()

export default request