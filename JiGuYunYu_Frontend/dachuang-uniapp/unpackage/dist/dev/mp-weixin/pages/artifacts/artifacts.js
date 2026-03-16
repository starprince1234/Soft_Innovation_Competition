"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_artifact = require("../../stores/artifact.js");
if (!Math) {
  (EmptyState + Loading)();
}
const EmptyState = () => "../../components/EmptyState/EmptyState.js";
const Loading = () => "../../components/Loading/Loading.js";
const _sfc_main = {
  __name: "artifacts",
  setup(__props) {
    const artifactStore = stores_artifact.useArtifactStore();
    const artifactList = common_vendor.ref([]);
    const loading = common_vendor.ref(false);
    const refreshing = common_vendor.ref(false);
    const hasMore = common_vendor.ref(true);
    const searchKeyword = common_vendor.ref("");
    const showFilter = common_vendor.ref(false);
    const filter = common_vendor.ref({
      period: "",
      material: ""
    });
    const pagination = common_vendor.ref({
      page: 1,
      pageSize: 20,
      total: 0
    });
    common_vendor.onMounted(() => {
      loadArtifacts();
    });
    const loadArtifacts = async (isRefresh = false) => {
      if (loading.value)
        return;
      if (isRefresh) {
        pagination.value.page = 1;
      }
      loading.value = true;
      try {
        const mockData = {
          data: {
            list: [
              {
                id: 1,
                name: "司母戊鼎",
                period: "商周",
                material: "青铜器",
                imageUrl: "https://via.placeholder.com/300x200/8B4513/FFF8DC?text=司母戊鼎",
                tags: ["国宝", "青铜器"]
              },
              {
                id: 2,
                name: "兵马俑",
                period: "秦汉",
                material: "陶瓷",
                imageUrl: "https://via.placeholder.com/300x200/8B4513/FFF8DC?text=兵马俑",
                tags: ["国宝", "陶瓷"]
              },
              {
                id: 3,
                name: "唐三彩",
                period: "隋唐",
                material: "陶瓷",
                imageUrl: "https://via.placeholder.com/300x200/8B4513/FFF8DC?text=唐三彩",
                tags: ["陶瓷", "唐代"]
              },
              {
                id: 4,
                name: "清明上河图",
                period: "宋元",
                material: "纸",
                imageUrl: "https://via.placeholder.com/300x200/8B4513/FFF8DC?text=清明上河图",
                tags: ["国宝", "绘画"]
              },
              {
                id: 5,
                name: "青花瓷",
                period: "明清",
                material: "陶瓷",
                imageUrl: "https://via.placeholder.com/300x200/8B4513/FFF8DC?text=青花瓷",
                tags: ["陶瓷", "明代"]
              },
              {
                id: 6,
                name: "金缕玉衣",
                period: "秦汉",
                material: "玉器",
                imageUrl: "https://via.placeholder.com/300x200/8B4513/FFF8DC?text=金缕玉衣",
                tags: ["国宝", "玉器"]
              }
            ],
            total: 6
          }
        };
        await new Promise((resolve) => setTimeout(resolve, 500));
        const res = mockData;
        if (isRefresh) {
          artifactList.value = res.data.list;
        } else {
          artifactList.value = [...artifactList.value, ...res.data.list];
        }
        pagination.value.total = res.data.total;
        hasMore.value = artifactList.value.length < res.data.total;
        artifactStore.setArtifactList(artifactList.value);
        artifactStore.setPagination(
          pagination.value.page,
          pagination.value.pageSize,
          pagination.value.total
        );
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/artifacts/artifacts.vue:290", "加载文物列表失败:", error);
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
        refreshing.value = false;
      }
    };
    const loadMore = () => {
      if (!hasMore.value || loading.value)
        return;
      pagination.value.page++;
      loadArtifacts();
    };
    const onRefresh = () => {
      refreshing.value = true;
      loadArtifacts(true);
    };
    const handleSearch = () => {
      loadArtifacts(true);
    };
    const applyFilter = () => {
      showFilter.value = false;
      loadArtifacts(true);
    };
    const resetFilter = () => {
      filter.value = {
        period: "",
        material: ""
      };
    };
    const goToDetail = (id) => {
      common_vendor.index.navigateTo({
        url: `/pages/artifact-detail/artifact-detail?id=${id}`
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(handleSearch),
        b: searchKeyword.value,
        c: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        d: common_vendor.o(($event) => showFilter.value = true),
        e: common_vendor.f(artifactList.value, (item, index, i0) => {
          return common_vendor.e({
            a: item.imageUrl,
            b: common_vendor.t(item.name),
            c: common_vendor.t(item.period),
            d: common_vendor.t(item.material),
            e: item.tags && item.tags.length > 0
          }, item.tags && item.tags.length > 0 ? {
            f: common_vendor.f(item.tags.slice(0, 2), (tag, tagIndex, i1) => {
              return {
                a: common_vendor.t(tag),
                b: tagIndex
              };
            })
          } : {}, {
            g: item.id || index,
            h: common_vendor.o(($event) => goToDetail(item.id), item.id || index)
          });
        }),
        f: artifactList.value.length === 0 && !loading.value
      }, artifactList.value.length === 0 && !loading.value ? {
        g: common_vendor.p({
          image: "/static/images/empty-artifacts.png",
          text: "暂无文物数据"
        })
      } : {}, {
        h: hasMore.value
      }, hasMore.value ? {
        i: common_vendor.t(loading.value ? "加载中..." : "上拉加载更多")
      } : {}, {
        j: !hasMore.value && artifactList.value.length > 0
      }, !hasMore.value && artifactList.value.length > 0 ? {} : {}, {
        k: common_vendor.o(loadMore),
        l: refreshing.value,
        m: common_vendor.o(onRefresh),
        n: common_vendor.o(($event) => showFilter.value = false),
        o: filter.value.period === "" ? 1 : "",
        p: common_vendor.o(($event) => filter.value.period = ""),
        q: filter.value.period === "商周" ? 1 : "",
        r: common_vendor.o(($event) => filter.value.period = "商周"),
        s: filter.value.period === "秦汉" ? 1 : "",
        t: common_vendor.o(($event) => filter.value.period = "秦汉"),
        v: filter.value.period === "隋唐" ? 1 : "",
        w: common_vendor.o(($event) => filter.value.period = "隋唐"),
        x: filter.value.period === "宋元" ? 1 : "",
        y: common_vendor.o(($event) => filter.value.period = "宋元"),
        z: filter.value.period === "明清" ? 1 : "",
        A: common_vendor.o(($event) => filter.value.period = "明清"),
        B: filter.value.material === "" ? 1 : "",
        C: common_vendor.o(($event) => filter.value.material = ""),
        D: filter.value.material === "青铜器" ? 1 : "",
        E: common_vendor.o(($event) => filter.value.material = "青铜器"),
        F: filter.value.material === "陶瓷" ? 1 : "",
        G: common_vendor.o(($event) => filter.value.material = "陶瓷"),
        H: filter.value.material === "玉器" ? 1 : "",
        I: common_vendor.o(($event) => filter.value.material = "玉器"),
        J: filter.value.material === "金银器" ? 1 : "",
        K: common_vendor.o(($event) => filter.value.material = "金银器"),
        L: common_vendor.o(resetFilter),
        M: common_vendor.o(applyFilter),
        N: common_vendor.o(() => {
        }),
        O: showFilter.value ? 1 : "",
        P: common_vendor.o(($event) => showFilter.value = false),
        Q: common_vendor.p({
          visible: loading.value && artifactList.value.length === 0,
          text: "加载中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2934441d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/artifacts/artifacts.js.map
