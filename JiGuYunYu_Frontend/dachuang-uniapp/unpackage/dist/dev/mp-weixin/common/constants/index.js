"use strict";
const API_BASE_URL = "https://your-api-domain.com/api/v1";
const API = {
  AUTH: {
    LOGIN: "/auth/login",
    REGISTER: "/auth/register",
    LOGOUT: "/auth/logout",
    REFRESH_TOKEN: "/auth/refresh",
    WECHAT_LOGIN: "/auth/wechat/login"
  },
  USER: {
    ME: "/users/me",
    UPDATE_PASSWORD: "/users/me/password"
  },
  DETECT: {
    CREATE_TASK: "/detect/tasks",
    TASK_STATUS: "/detect/tasks",
    TASK_RESULT: "/detect/results"
  },
  DIALOG: {
    CREATE_REQUEST: "/dialog/requests",
    GET_RESULT: "/dialog/results",
    HISTORIES: "/dialog/histories"
  },
  ARTIFACTS: {
    LIST: "/artifacts",
    DETAIL: "/artifact"
  },
  FEEDBACK: {
    SUBMIT: "/feedback"
  },
  HEALTH: "/health"
};
const STORAGE_KEYS = {
  TOKEN: "token",
  REFRESH_TOKEN: "refreshToken",
  USER_INFO: "userInfo",
  CHAT_HISTORY: "chatHistory"
};
const IMAGE_QUALITY = {
  HIGH: 0.8,
  MEDIUM: 0.6,
  LOW: 0.4
};
const MAX_IMAGE_SIZE = 10 * 1024 * 1024;
const POLLING_INTERVAL = 2e3;
const MAX_POLLING_TIMES = 30;
exports.API = API;
exports.API_BASE_URL = API_BASE_URL;
exports.IMAGE_QUALITY = IMAGE_QUALITY;
exports.MAX_IMAGE_SIZE = MAX_IMAGE_SIZE;
exports.MAX_POLLING_TIMES = MAX_POLLING_TIMES;
exports.POLLING_INTERVAL = POLLING_INTERVAL;
exports.STORAGE_KEYS = STORAGE_KEYS;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/common/constants/index.js.map
