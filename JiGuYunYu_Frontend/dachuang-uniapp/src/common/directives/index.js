export default {
  install(app) {
    app.config.globalProperties.$formatDate = (date, format) => {
      if (!date) return ''
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')

      return format
        .replace('YYYY', year)
        .replace('MM', month)
        .replace('DD', day)
        .replace('HH', hours)
        .replace('mm', minutes)
        .replace('ss', seconds)
    }

    app.config.globalProperties.$getRoleName = (role) => {
      const roleMap = {
        'USER': '普通用户',
        'ARCHAEOLOGIST': '考古人员',
        'ADMIN': '管理员'
      }
      return roleMap[role] || '未知角色'
    }

    app.config.globalProperties.$getStatusText = (status) => {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝'
      }
      return statusMap[status] || status
    }

    app.config.globalProperties.$getStatusColor = (status) => {
      const colorMap = {
        'PENDING': '#FFA500',
        'APPROVED': '#52C41A',
        'REJECTED': '#FF4D4F'
      }
      return colorMap[status] || '#999999'
    }

    app.config.globalProperties.$truncateText = (text, maxLength = 50) => {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    }
  }
}