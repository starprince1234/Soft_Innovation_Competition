"use strict";
const common_vendor = require("../vendor.js");
const common_constants_index = require("../constants/index.js");
class Request {
  constructor() {
    this.baseURL = common_constants_index.API_BASE_URL;
    this.timeout = 3e4;
  }
  getHeader() {
    const token = common_vendor.index.getStorageSync(common_constants_index.STORAGE_KEYS.TOKEN);
    return {
      "Content-Type": "application/json",
      "Authorization": token ? `Bearer ${token}` : ""
    };
  }
  request(options) {
    return new Promise((resolve, reject) => {
      common_vendor.index.request({
        url: this.baseURL + options.url,
        method: options.method || "GET",
        data: options.data || {},
        header: {
          ...this.getHeader(),
          ...options.header
        },
        timeout: this.timeout,
        success: (res) => {
          const { statusCode, data } = res;
          const message = (data == null ? void 0 : data.message) || (data == null ? void 0 : data.msg) || "请求失败";
          if (statusCode >= 200 && statusCode < 300) {
            if ((data == null ? void 0 : data.code) === 200) {
              resolve(data);
            } else {
              this.handleError(data);
              reject({
                code: data == null ? void 0 : data.code,
                message,
                data: data == null ? void 0 : data.data
              });
            }
          } else if (statusCode === 401) {
            this.handleError({ message });
            this.handleUnauthorized();
            reject({ code: 401, message });
          } else {
            this.handleError({ message });
            reject({ code: statusCode, message });
          }
        },
        fail: (err) => {
          console.error("请求失败:", err);
          common_vendor.index.showToast({
            title: "网络请求失败",
            icon: "none"
          });
          reject(err);
        }
      });
    });
  }
  get(url, params = {}) {
    return this.request({
      url,
      method: "GET",
      data: params
    });
  }
  post(url, data = {}) {
    return this.request({
      url,
      method: "POST",
      data
    });
  }
  put(url, data = {}) {
    return this.request({
      url,
      method: "PUT",
      data
    });
  }
  delete(url, data = {}) {
    return this.request({
      url,
      method: "DELETE",
      data
    });
  }
  handleError(data) {
    const message = data.message || "请求失败";
    common_vendor.index.showToast({
      title: message,
      icon: "none",
      duration: 2e3
    });
  }
  handleUnauthorized() {
    common_vendor.index.removeStorageSync(common_constants_index.STORAGE_KEYS.TOKEN);
    common_vendor.index.removeStorageSync(common_constants_index.STORAGE_KEYS.USER_INFO);
    common_vendor.index.reLaunch({
      url: "/pages/login/login"
    });
  }
}
const request = new Request();
exports.request = request;
