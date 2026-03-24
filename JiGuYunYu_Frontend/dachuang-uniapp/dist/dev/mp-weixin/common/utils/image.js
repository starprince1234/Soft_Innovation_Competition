"use strict";
const common_vendor = require("../vendor.js");
const common_constants_index = require("../constants/index.js");
const chooseImage = (count = 1, sizeType = ["compressed"]) => {
  return new Promise((resolve, reject) => {
    common_vendor.index.chooseImage({
      count,
      sizeType,
      sourceType: ["album", "camera"],
      success: (res) => {
        resolve(res.tempFilePaths);
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
};
const compressImage = (filePath, quality = common_constants_index.IMAGE_QUALITY.MEDIUM) => {
  return new Promise((resolve, reject) => {
    common_vendor.index.compressImage({
      src: filePath,
      quality,
      success: (res) => {
        resolve(res.tempFilePath);
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
};
const getImageInfo = (filePath) => {
  return new Promise((resolve, reject) => {
    common_vendor.index.getImageInfo({
      src: filePath,
      success: (res) => {
        resolve(res);
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
};
const fileToBase64 = (filePath) => {
  return new Promise((resolve, reject) => {
    common_vendor.index.getFileSystemManager().readFile({
      filePath,
      encoding: "base64",
      success: (res) => {
        resolve(res.data);
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
};
const validateImageSize = (fileSize) => {
  return fileSize <= common_constants_index.MAX_IMAGE_SIZE;
};
const processImageForUpload = async (filePath) => {
  try {
    const imageInfo = await getImageInfo(filePath);
    if (!validateImageSize(imageInfo.size)) {
      throw new Error("图片大小不能超过10MB");
    }
    let processedPath = filePath;
    if (imageInfo.width > 2e3 || imageInfo.height > 2e3) {
      processedPath = await compressImage(filePath, common_constants_index.IMAGE_QUALITY.MEDIUM);
    }
    const base64 = await fileToBase64(processedPath);
    return {
      base64,
      width: imageInfo.width,
      height: imageInfo.height,
      size: imageInfo.size
    };
  } catch (error) {
    console.error("图片处理失败:", error);
    throw error;
  }
};
exports.chooseImage = chooseImage;
exports.processImageForUpload = processImageForUpload;
