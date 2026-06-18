<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user.js'
import { prefetchAssets, setRemoteStaticAssetsEnabled } from '@/common/utils/asset-cache.js'

const CORE_ASSET_PATHS = [
  '/static/images/logo.png',
  '/static/images/default-avatar.png',
  '/static/images/empty.png',
  '/static/images/empty-result.png',
  '/static/images/empty-chat.png',
  '/static/images/empty-artifacts.png',
  '/static/images/banner1.png',
  '/static/images/banner2.png',
  '/static/images/ai-avatar.png',
  '/static/icons/about.svg',
  '/static/icons/add.svg',
  '/static/icons/artifacts.svg',
  '/static/icons/chat.svg',
  '/static/icons/chevron-down.svg',
  '/static/icons/chevron-right.svg',
  '/static/icons/chevron-up.svg',
  '/static/icons/close.svg',
  '/static/icons/clock.svg',
  '/static/icons/edit.svg',
  '/static/icons/email.svg',
  '/static/icons/favorite.svg',
  '/static/icons/feedback-bug.svg',
  '/static/icons/feedback-content.svg',
  '/static/icons/feedback-feature.svg',
  '/static/icons/feedback-general.svg',
  '/static/icons/feedback.svg',
  '/static/icons/filter.svg',
  '/static/icons/help.svg',
  '/static/icons/history.svg',
  '/static/icons/mobile.svg',
  '/static/icons/phone.svg',
  '/static/icons/scan.svg',
  '/static/icons/search.svg',
  '/static/icons/settings.svg',
  '/static/icons/share.svg',
  '/static/icons/star-filled.svg',
  '/static/icons/star-outline.svg',
  '/static/icons/trash.svg'
]

onLaunch(() => {
  console.log('App Launch')
  setRemoteStaticAssetsEnabled(true)
  prefetchAssets(CORE_ASSET_PATHS)
  const userStore = useUserStore()
  userStore.initFromStorage()
  checkLogin()
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})

const checkLogin = () => {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.reLaunch({
      url: '/pages/login/login'
    })
  }
}
</script>

<style lang="scss">
@import '@/common/styles/index.scss';

page {
  background-color: #FFF8F0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}
</style>