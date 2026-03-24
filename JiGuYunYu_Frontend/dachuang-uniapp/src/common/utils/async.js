import { POLLING_INTERVAL, MAX_POLLING_TIMES } from '@/common/constants/index.js'

export const pollTaskStatus = async (taskId, statusCheckFn, resultFetchFn) => {
  let pollCount = 0

  return new Promise((resolve, reject) => {
    const poll = async () => {
      try {
        pollCount++

        if (pollCount > MAX_POLLING_TIMES) {
          reject(new Error('任务处理超时，请稍后重试'))
          return
        }

        const status = await statusCheckFn(taskId)

        if (status === 'COMPLETED') {
          const result = await resultFetchFn(taskId)
          resolve(result)
        } else if (status === 'FAILED') {
          reject(new Error('任务处理失败'))
        } else {
          setTimeout(poll, POLLING_INTERVAL)
        }
      } catch (error) {
        reject(error)
      }
    }

    poll()
  })
}

export const sleep = (ms) => {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export const retry = async (fn, maxRetries = 3, delay = 1000) => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn()
    } catch (error) {
      if (i === maxRetries - 1) {
        throw error
      }
      await sleep(delay * (i + 1))
    }
  }
}