"use strict";
const common_constants_index = require("../constants/index.js");
const pollTaskStatus = async (taskId, statusCheckFn, resultFetchFn) => {
  let pollCount = 0;
  return new Promise((resolve, reject) => {
    const poll = async () => {
      try {
        pollCount++;
        if (pollCount > common_constants_index.MAX_POLLING_TIMES) {
          reject(new Error("任务处理超时，请稍后重试"));
          return;
        }
        const status = await statusCheckFn(taskId);
        if (status === "COMPLETED") {
          const result = await resultFetchFn(taskId);
          resolve(result);
        } else if (status === "FAILED") {
          reject(new Error("任务处理失败"));
        } else {
          setTimeout(poll, common_constants_index.POLLING_INTERVAL);
        }
      } catch (error) {
        reject(error);
      }
    };
    poll();
  });
};
exports.pollTaskStatus = pollTaskStatus;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/common/utils/async.js.map
