<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user.js'

onLaunch(() => {
  console.log('App Launch')
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