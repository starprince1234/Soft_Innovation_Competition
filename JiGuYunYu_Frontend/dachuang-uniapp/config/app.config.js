export default {
  pages: [
    'pages/index/index',
    'pages/login/login',
    'pages/register/register',
    'pages/result/result',
    'pages/artifacts/artifacts',
    'pages/chat/chat',
    'pages/feedback/feedback',
    'pages/profile/profile',
    'pages/artifact-detail/artifact-detail'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#C8102E',
    navigationBarTitleText: '文物识别',
    navigationBarTextStyle: 'white',
    backgroundColor: '#F5F5F5'
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#C8102E',
    backgroundColor: '#FFFFFF',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '识别',
        iconPath: 'static/tabbar/scan.png',
        selectedIconPath: 'static/tabbar/scan-active.png'
      },
      {
        pagePath: 'pages/artifacts/artifacts',
        text: '文物库',
        iconPath: 'static/tabbar/artifacts.png',
        selectedIconPath: 'static/tabbar/artifacts-active.png'
      },
      {
        pagePath: 'pages/chat/chat',
        text: 'AI问答',
        iconPath: 'static/tabbar/chat.png',
        selectedIconPath: 'static/tabbar/chat-active.png'
      },
      {
        pagePath: 'pages/profile/profile',
        text: '我的',
        iconPath: 'static/tabbar/profile.png',
        selectedIconPath: 'static/tabbar/profile-active.png'
      }
    ]
  },
  usingComponents: {},
  permission: {
    'scope.userLocation': {
      desc: '您的位置信息将用于小程序位置接口的效果展示'
    }
  },
  networkTimeout: {
    request: 30000,
    downloadFile: 30000,
    uploadFile: 30000
  },
  debug: false
}