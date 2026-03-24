import { API_BASE_URL, STORAGE_KEYS } from '@/common/constants/index.js'

class Request {
  constructor() {
    this.baseURL = API_BASE_URL
    this.timeout = 30000
  }

  sanitizeQueryParams(params = {}) {
    const cleaned = {}

    Object.keys(params).forEach((key) => {
      const value = params[key]
      if (value === undefined || value === null) return

      if (typeof value === 'string') {
        const trimmed = value.trim()
        if (!trimmed) return
        const lowered = trimmed.toLowerCase()
        if (lowered === 'undefined' || lowered === 'null') return
      }

      cleaned[key] = value
    })

    return cleaned
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
      const requestTimeout = Number(options.timeout) || this.timeout

      uni.request({
        url: this.baseURL + options.url,
        method: options.method || 'GET',
        data: options.data || {},
        header: {
          ...this.getHeader(),
          ...options.header
        },
        timeout: requestTimeout,
        success: (res) => {
          const { statusCode, data } = res
          const message = data?.message || data?.msg || '请求失败'

          if (statusCode >= 200 && statusCode < 300) {
            if (data?.code === 200) {
              resolve(data)
            } else {
              this.handleError(data)
              reject({
                code: data?.code,
                message,
                data: data?.data
              })
            }
          } else if (statusCode === 401) {
            this.handleError({ message })
            this.handleUnauthorized()
            reject({ code: 401, message })
          } else {
            this.handleError({ message: message || `请求失败，状态码：${statusCode}` })
            reject({ code: statusCode, message: message || `请求失败，状态码：${statusCode}` })
          }
        },
        fail: (err) => {
          console.error('请求失败:', err)

          const isTimeout = String(err?.errMsg || '').toLowerCase().includes('timeout')
          reject({
            ...err,
            code: isTimeout ? 'REQUEST_TIMEOUT' : 'REQUEST_FAILED',
            message: isTimeout ? '请求超时，请稍后重试' : '网络请求失败'
          })
        }
      })
    })
  }

  get(url, params = {}, requestOptions = {}) {
    return this.request({
      url,
      method: 'GET',
      data: this.sanitizeQueryParams(params),
      ...requestOptions
    })
  }

  post(url, data = {}, requestOptions = {}) {
    return this.request({
      url,
      method: 'POST',
      data,
      ...requestOptions
    })
  }

  put(url, data = {}, requestOptions = {}) {
    return this.request({
      url,
      method: 'PUT',
      data,
      ...requestOptions
    })
  }

  delete(url, data = {}, requestOptions = {}) {
    return this.request({
      url,
      method: 'DELETE',
      data,
      ...requestOptions
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
    uni.removeStorageSync(STORAGE_KEYS.USER_INFO)
    uni.reLaunch({
      url: '/pages/login/login'
    })
  }
}

const request = new Request()

export default request