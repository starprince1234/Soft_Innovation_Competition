export const API_BASE_URL = 'https://your-api-domain.com/api/v1'

export const API = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    LOGOUT: '/auth/logout',
    REFRESH_TOKEN: '/auth/refresh',
    WECHAT_LOGIN: '/auth/wechat/login',
    EMAIL_LOGIN: '/auth/email/login',
    SEND_CODE: '/auth/send-code'
  },
  USER: {
    ME: '/users/me',
    UPDATE_PASSWORD: '/users/me/password'
  },
  DETECT: {
    CREATE_TASK: '/detect/tasks',
    TASK_STATUS: '/detect/tasks',
    TASK_RESULT: '/detect/results'
  },
  DIALOG: {
    CREATE_REQUEST: '/dialog/requests',
    GET_RESULT: '/dialog/results',
    HISTORIES: '/dialog/histories'
  },
  ARTIFACTS: {
    LIST: '/artifacts',
    DETAIL: '/artifact'
  },
  FEEDBACK: {
    SUBMIT: '/feedback'
  },
  HEALTH: '/health'
}

export const STORAGE_KEYS = {
  TOKEN: 'token',
  REFRESH_TOKEN: 'refreshToken',
  USER_INFO: 'userInfo',
  CHAT_HISTORY: 'chatHistory'
}

export const USER_ROLES = {
  PUBLIC: 'PUBLIC',
  ARCHAEOLOGIST: 'ARCHAEOLOGIST',
  MANAGER: 'MANAGER'
}

export const TASK_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
}

export const ARTIFACT_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
}

export const DIALOG_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
}

export const CHAT_TYPES = {
  USER: 'user',
  AI: 'ai'
}

export const IMAGE_QUALITY = {
  HIGH: 0.8,
  MEDIUM: 0.6,
  LOW: 0.4
}

export const MAX_IMAGE_SIZE = 10 * 1024 * 1024

export const POLLING_INTERVAL = 2000

export const MAX_POLLING_TIMES = 30