"use strict";
const formatDate = (date, format = "YYYY-MM-DD HH:mm:ss") => {
  if (!date)
    return "";
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  const seconds = String(d.getSeconds()).padStart(2, "0");
  return format.replace("YYYY", year).replace("MM", month).replace("DD", day).replace("HH", hours).replace("mm", minutes).replace("ss", seconds);
};
const validatePhone = (phone) => {
  return /^1[3-9]\d{9}$/.test(phone);
};
const validateEmail = (email) => {
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
};
const validatePassword = (password) => {
  return password.length >= 6;
};
const getRoleName = (role) => {
  const roleMap = {
    "PUBLIC": "公众用户",
    "ARCHAEOLOGIST": "考古人员",
    "MANAGER": "博物馆管理者"
  };
  return roleMap[role] || "未知角色";
};
const getStatusText = (status) => {
  const statusMap = {
    "PENDING": "待审核",
    "APPROVED": "已通过",
    "REJECTED": "已拒绝"
  };
  return statusMap[status] || status;
};
const getStatusColor = (status) => {
  const colorMap = {
    "PENDING": "#FFA500",
    "APPROVED": "#52C41A",
    "REJECTED": "#FF4D4F"
  };
  return colorMap[status] || "#999999";
};
const parseMarkdown = (text) => {
  if (!text)
    return "";
  return text.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>").replace(/\*(.*?)\*/g, "<em>$1</em>").replace(/`(.*?)`/g, "<code>$1</code>").replace(/\n/g, "<br>");
};
exports.formatDate = formatDate;
exports.getRoleName = getRoleName;
exports.getStatusColor = getStatusColor;
exports.getStatusText = getStatusText;
exports.parseMarkdown = parseMarkdown;
exports.validateEmail = validateEmail;
exports.validatePassword = validatePassword;
exports.validatePhone = validatePhone;
