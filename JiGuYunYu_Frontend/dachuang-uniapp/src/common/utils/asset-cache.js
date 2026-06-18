import manifest from '@/common/constants/assets-manifest.json'

const STORAGE_KEY = 'asset_cache_map_v1'
const REMOTE_STATIC_ASSET_FLAG = 'enable_remote_static_assets'
const inflightDownloads = new Map()

const readCacheMap = () => {
  return uni.getStorageSync(STORAGE_KEY) || {}
}

const writeCacheMap = (cacheMap) => {
  uni.setStorageSync(STORAGE_KEY, cacheMap)
}

export const getAssetCacheMap = () => {
  return readCacheMap()
}

export const setRemoteStaticAssetsEnabled = (enabled) => {
  uni.setStorageSync(REMOTE_STATIC_ASSET_FLAG, enabled === true)
}

export const toRemoteAssetUrl = (assetPath) => {
  const remoteUrl = manifest[assetPath]
  if (!remoteUrl) return assetPath

  const remoteEnabled = uni.getStorageSync(REMOTE_STATIC_ASSET_FLAG) !== false
  if (!remoteEnabled && remoteUrl.includes('/assets/weixin/')) {
    return assetPath
  }

  return remoteUrl
}

const getRemoteUrlForDownload = (assetPath) => {
  const remoteUrl = manifest[assetPath]
  if (!remoteUrl) return ''
  return remoteUrl
}

export const resolveAssetPath = (assetPath) => {
  const cacheMap = readCacheMap()
  const remoteUrl = getRemoteUrlForDownload(assetPath)
  // Never render local /static paths first on mini program pages.
  // Use cache if available, otherwise use OSS URL and cache in background.
  const resolved = cacheMap[assetPath] || remoteUrl || assetPath

  if (!cacheMap[assetPath] && remoteUrl) {
    ensureAssetCachedBackground(assetPath)
  }

  return resolved
}

export const ensureAssetCached = async (assetPath) => {
  const remoteUrl = getRemoteUrlForDownload(assetPath)
  if (!remoteUrl || remoteUrl === assetPath) {
    return resolveAssetPath(assetPath)
  }

  const cacheMap = readCacheMap()
  if (cacheMap[assetPath]) {
    return cacheMap[assetPath]
  }

  if (inflightDownloads.has(assetPath)) {
    return inflightDownloads.get(assetPath)
  }

  const downloadPromise = (async () => {
    console.log('[asset-cache] download start', assetPath, remoteUrl)
    const downloadRes = await new Promise((resolve, reject) => {
      uni.downloadFile({
        url: remoteUrl,
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(res)
          } else {
            reject(new Error(`下载资源失败: ${res.statusCode}`))
          }
        },
        fail: reject
      })
    })

    const saveRes = await new Promise((resolve, reject) => {
      uni.saveFile({
        tempFilePath: downloadRes.tempFilePath,
        success: resolve,
        fail: reject
      })
    })

    cacheMap[assetPath] = saveRes.savedFilePath
    writeCacheMap(cacheMap)
    console.log('[asset-cache] cache saved', assetPath, saveRes.savedFilePath)
    return saveRes.savedFilePath
  })()

  inflightDownloads.set(assetPath, downloadPromise)
  try {
    return await downloadPromise
  } finally {
    inflightDownloads.delete(assetPath)
  }
}

export const ensureAssetCachedBackground = (assetPath) => {
  ensureAssetCached(assetPath).catch((error) => {
    console.warn('静态资源缓存失败:', assetPath, error)
  })
}

export const prefetchAssets = async (assetPaths = []) => {
  for (const assetPath of assetPaths) {
    try {
      await ensureAssetCached(assetPath)
    } catch (error) {
      console.warn('静态资源缓存失败:', assetPath, error)
    }
  }
}

const getFileInfo = (filePath) => new Promise((resolve, reject) => {
  uni.getFileInfo({
    filePath,
    success: resolve,
    fail: reject
  })
})

const removeSavedFile = (filePath) => new Promise((resolve, reject) => {
  uni.removeSavedFile({
    filePath,
    success: resolve,
    fail: reject
  })
})

const getSavedFileList = () => new Promise((resolve, reject) => {
  uni.getSavedFileList({
    success: resolve,
    fail: reject
  })
})

const collectKnownSavedFilePaths = async () => {
  const cacheMap = readCacheMap()
  const paths = new Set(Object.values(cacheMap).filter(Boolean))

  try {
    const saved = await getSavedFileList()
    ;(saved?.fileList || []).forEach((item) => {
      if (item?.filePath) {
        paths.add(item.filePath)
      }
    })
  } catch (error) {
    console.warn('读取已保存文件列表失败:', error)
  }

  return [...paths]
}

export const getAssetCacheSizeBytes = async () => {
  const filePaths = await collectKnownSavedFilePaths()

  let totalSize = 0
  for (const filePath of filePaths) {
    try {
      const info = await getFileInfo(filePath)
      totalSize += Number(info.size) || 0
    } catch (error) {
      console.warn('读取缓存文件大小失败:', filePath, error)
    }
  }

  return totalSize
}

export const clearAssetCacheFiles = async () => {
  const filePaths = await collectKnownSavedFilePaths()

  for (const filePath of filePaths) {
    try {
      await removeSavedFile(filePath)
    } catch (error) {
      console.warn('删除缓存文件失败:', filePath, error)
    }
  }

  uni.removeStorageSync(STORAGE_KEY)
}
