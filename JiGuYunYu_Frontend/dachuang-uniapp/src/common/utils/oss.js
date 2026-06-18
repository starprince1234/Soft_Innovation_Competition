const OSS_BASE_URL = 'https://jiguyunyu1.oss-cn-hangzhou.aliyuncs.com'

const joinOssUrl = (path) => {
  const normalizedPath = String(path || '').trim().replace(/^\/+/, '')
  if (!normalizedPath) return ''
  return `${OSS_BASE_URL}/${normalizedPath}`
}

export const normalizeOssUrl = (value, fallback = '') => {
  if (!value) return fallback

  const raw = String(value).trim()
  if (!raw) return fallback

  // Already a full URL.
  if (/^https?:\/\//i.test(raw)) {
    return encodeURI(raw)
  }

  return encodeURI(joinOssUrl(raw))
}

const looksLikeOssTextObject = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return false

  return /\.txt(\?|$)/i.test(raw)
}

export const loadArtifactDescription = async (descriptionValue) => {
  if (!descriptionValue) return ''
  if (!looksLikeOssTextObject(descriptionValue)) return descriptionValue

  const textUrl = normalizeOssUrl(descriptionValue, '')
  if (!textUrl) return descriptionValue

  try {
    const response = await new Promise((resolve, reject) => {
      uni.request({
        url: textUrl,
        method: 'GET',
        dataType: 'text',
        responseType: 'text',
        header: {
          Accept: 'text/plain,text/*,*/*'
        },
        success: resolve,
        fail: reject
      })
    })

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (typeof response.data === 'string') {
        return response.data
      }
      return JSON.stringify(response.data)
    }
  } catch (error) {
    console.warn('加载文物详情文本失败，使用原始描述:', error)
  }

  return descriptionValue
}
