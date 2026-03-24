import { MAX_IMAGE_SIZE, IMAGE_QUALITY } from '@/common/constants/index.js'

export const chooseImage = (count = 1, sizeType = ['compressed']) => {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sizeType,
      sourceType: ['album', 'camera'],
      success: (res) => {
        resolve(res.tempFilePaths)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export const compressImage = (filePath, quality = IMAGE_QUALITY.MEDIUM) => {
  return new Promise((resolve, reject) => {
    uni.compressImage({
      src: filePath,
      quality,
      success: (res) => {
        resolve(res.tempFilePath)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export const getImageInfo = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.getImageInfo({
      src: filePath,
      success: (res) => {
        resolve(res)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export const getFileInfo = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.getFileSystemManager().getFileInfo({
      filePath,
      success: (res) => {
        resolve(res)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export const fileToBase64 = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.getFileSystemManager().readFile({
      filePath,
      encoding: 'base64',
      success: (res) => {
        resolve(res.data)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export const validateImageSize = (fileSize) => {
  return fileSize <= MAX_IMAGE_SIZE
}

export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

export const processImageForUpload = async (filePath) => {
  try {
    const imageInfo = await getImageInfo(filePath)

    // 微信端 getImageInfo 不保证包含 size，统一以文件系统大小为准。
    let realSize = Number(imageInfo?.size)
    if (!Number.isFinite(realSize) || realSize <= 0) {
      const fileInfo = await getFileInfo(filePath)
      realSize = Number(fileInfo?.size)
    }

    if (!Number.isFinite(realSize) || realSize <= 0) {
      throw new Error('图片读取失败，请重新选择图片')
    }

    if (!validateImageSize(realSize)) {
      throw new Error('图片大小不能超过10MB')
    }

    let processedPath = filePath
    
    if (imageInfo.width > 2000 || imageInfo.height > 2000) {
      processedPath = await compressImage(filePath, IMAGE_QUALITY.MEDIUM)
    }

    const base64 = await fileToBase64(processedPath)
    
    return {
      base64,
      width: imageInfo.width,
      height: imageInfo.height,
      size: realSize
    }
  } catch (error) {
    console.error('图片处理失败:', error)
    throw error
  }
}

export const previewImage = (urls, current = 0) => {
  uni.previewImage({
    urls,
    current
  })
}

export const saveImageToPhotosAlbum = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.saveImageToPhotosAlbum({
      filePath,
      success: () => {
        uni.showToast({
          title: '已保存到相册',
          icon: 'success'
        })
        resolve()
      },
      fail: (err) => {
        if (err.errMsg.includes('auth deny')) {
          uni.showModal({
            title: '提示',
            content: '需要您授权保存相册权限',
            success: (modalRes) => {
              if (modalRes.confirm) {
                uni.openSetting()
              }
            }
          })
        }
        reject(err)
      }
    })
  })
}