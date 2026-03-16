import request from '@/common/utils/request.js'
import { API } from '@/common/constants/index.js'

export const authApi = {
  login(data) {
    return request.post(API.AUTH.LOGIN, data)
  },

  register(data) {
    return request.post(API.AUTH.REGISTER, data)
  },

  logout() {
    return request.post(API.AUTH.LOGOUT)
  },

  refreshToken(data) {
    return request.post(API.AUTH.REFRESH_TOKEN, data)
  },

  wechatLogin(data) {
    return request.post(API.AUTH.WECHAT_LOGIN, data)
  },

  emailLogin(data) {
    return request.post(API.AUTH.EMAIL_LOGIN, data)
  },

  sendCode(data) {
    return request.post(API.AUTH.SEND_CODE, data)
  }
}

export const userApi = {
  getMe() {
    return request.get(API.USER.ME)
  },

  updatePassword(data) {
    return request.put(API.USER.UPDATE_PASSWORD, data)
  }
}

export const detectApi = {
  createTask(data) {
    return request.post(API.DETECT.CREATE_TASK, data)
  },

  getTaskStatus(taskId) {
    return request.get(`${API.DETECT.TASK_STATUS}/${taskId}/status`)
  },

  getTaskResult(taskId) {
    return request.get(`${API.DETECT.TASK_RESULT}/${taskId}`)
  }
}

export const dialogApi = {
  createRequest(data) {
    return request.post(API.DIALOG.CREATE_REQUEST, data)
  },

  getResult(dialogTaskId) {
    return request.get(`${API.DIALOG.GET_RESULT}/${dialogTaskId}`)
  },

  getHistories(params) {
    return request.get(API.DIALOG.HISTORIES, params)
  },

  clearHistories(artifactId) {
    const params = artifactId ? { artifactId } : {}
    return request.delete(API.DIALOG.HISTORIES, params)
  }
}

export const artifactsApi = {
  getList(params) {
    return request.get(API.ARTIFACTS.LIST, params)
  },

  getDetail(id) {
    return request.get(`${API.ARTIFACTS.DETAIL}/${id}`)
  }
}

export const feedbackApi = {
  submit(data) {
    return request.post(API.FEEDBACK.SUBMIT, data)
  }
}

export const healthApi = {
  check() {
    return request.get(API.HEALTH)
  }
}