"use strict";
const common_utils_request = require("../utils/request.js");
const common_constants_index = require("../constants/index.js");
const authApi = {
  login(data) {
    return common_utils_request.request.post(common_constants_index.API.AUTH.LOGIN, data);
  },
  register(data) {
    return common_utils_request.request.post(common_constants_index.API.AUTH.REGISTER, data);
  },
  logout() {
    return common_utils_request.request.post(common_constants_index.API.AUTH.LOGOUT);
  }
};
const userApi = {
  getMe() {
    return common_utils_request.request.get(common_constants_index.API.USER.ME);
  },
  updatePassword(data) {
    return common_utils_request.request.put(common_constants_index.API.USER.UPDATE_PASSWORD, data);
  }
};
const detectApi = {
  createTask(data) {
    return common_utils_request.request.post(common_constants_index.API.DETECT.CREATE_TASK, data);
  },
  getTaskStatus(taskId) {
    return common_utils_request.request.get(`${common_constants_index.API.DETECT.TASK_STATUS}/${taskId}/status`);
  },
  getTaskResult(taskId) {
    return common_utils_request.request.get(`${common_constants_index.API.DETECT.TASK_RESULT}/${taskId}`);
  }
};
const dialogApi = {
  createRequest(data) {
    return common_utils_request.request.post(common_constants_index.API.DIALOG.CREATE_REQUEST, data);
  },
  getResult(dialogId) {
    return common_utils_request.request.get(`${common_constants_index.API.DIALOG.GET_RESULT}/${dialogId}`);
  },
  getHistories(params) {
    return common_utils_request.request.get(common_constants_index.API.DIALOG.HISTORIES, params);
  },
  clearHistories(artifactId) {
    const params = artifactId ? { artifactId } : {};
    return common_utils_request.request.delete(common_constants_index.API.DIALOG.HISTORIES, params);
  }
};
const artifactsApi = {
  getList(params) {
    return common_utils_request.request.get(common_constants_index.API.ARTIFACTS.LIST, params);
  },
  getDetail(id) {
    return common_utils_request.request.get(`${common_constants_index.API.ARTIFACTS.DETAIL}/${id}`);
  }
};
const feedbackApi = {
  submit(data) {
    return common_utils_request.request.post(common_constants_index.API.FEEDBACK.SUBMIT, data);
  }
};
exports.artifactsApi = artifactsApi;
exports.authApi = authApi;
exports.detectApi = detectApi;
exports.dialogApi = dialogApi;
exports.feedbackApi = feedbackApi;
exports.userApi = userApi;
