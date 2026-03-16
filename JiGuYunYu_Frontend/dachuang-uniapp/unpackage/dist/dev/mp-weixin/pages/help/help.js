"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "help",
  setup(__props) {
    const searchKeyword = common_vendor.ref("");
    const faqs = common_vendor.ref([
      {
        question: "如何使用文物识别功能？",
        answer: '打开应用后，点击首页的"开始识别"按钮，拍摄或选择一张文物照片，系统会自动分析并给出识别结果。',
        expanded: false
      },
      {
        question: "识别结果不准确怎么办？",
        answer: "请确保拍摄的文物照片清晰，光线充足，尽量避免反光和遮挡。如果识别结果仍然不准确，您可以通过反馈功能告诉我们。",
        expanded: false
      },
      {
        question: "如何收藏文物？",
        answer: "在文物详情页面，点击右上角的收藏按钮即可将文物添加到收藏列表。",
        expanded: false
      },
      {
        question: "如何查看我的识别记录？",
        answer: '在个人中心页面，点击"识别记录"即可查看您的历史识别记录。',
        expanded: false
      },
      {
        question: "如何修改个人信息？",
        answer: "在个人中心页面，点击头像或用户名进入个人资料编辑页面，即可修改您的个人信息。",
        expanded: false
      }
    ]);
    const filteredFaqs = common_vendor.computed(() => {
      if (!searchKeyword.value) {
        return faqs.value;
      }
      return faqs.value.filter(
        (faq) => faq.question.includes(searchKeyword.value) || faq.answer.includes(searchKeyword.value)
      );
    });
    const toggleFaq = (index) => {
      faqs.value[index].expanded = !faqs.value[index].expanded;
    };
    const openGuide = (title) => {
      common_vendor.index.showToast({
        title: `${title}功能开发中`,
        icon: "none"
      });
    };
    return (_ctx, _cache) => {
      return {
        a: searchKeyword.value,
        b: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        c: common_vendor.f(filteredFaqs.value, (faq, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(faq.question),
            b: common_vendor.t(faq.expanded ? "▼" : "▶"),
            c: common_vendor.o(($event) => toggleFaq(index), index),
            d: faq.expanded
          }, faq.expanded ? {
            e: common_vendor.t(faq.answer)
          } : {}, {
            f: index
          });
        }),
        d: common_vendor.o(($event) => openGuide("识别指南")),
        e: common_vendor.o(($event) => openGuide("收藏指南")),
        f: common_vendor.o(($event) => openGuide("问答指南"))
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-5194e907"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/help/help.js.map
