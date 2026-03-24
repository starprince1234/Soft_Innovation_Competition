"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_artifact = require("../../stores/artifact.js");
const common_api_index = require("../../common/api/index.js");
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
      page: 0,
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
        pagination.value.page = 0;
      }
      loading.value = true;
      try {
        const params = {
          page: pagination.value.page,
          size: pagination.value.pageSize,
          keyword: searchKeyword.value || void 0,
          era: filter.value.period || void 0,
          tags: filter.value.material || void 0
        };
        const res = await common_api_index.artifactsApi.getList(params);
        const pageData = res.data;
        const mappedList = (pageData.list || []).map((item) => ({
          id: item.id,
          name: item.name,
          period: item.era,
          material: item.category,
          imageUrl: item.thumbnailUrl || item.imageUrl,
          tags: item.tags || [],
          location: item.location,
          status: item.status,
          description: item.description
        }));
        if (isRefresh) {
          artifactList.value = mappedList;
        } else {
          artifactList.value = [...artifactList.value, ...mappedList];
        }
        pagination.value.total = pageData.totalElements || 0;
        pagination.value.page = pageData.currentPage || pagination.value.page;
        pagination.value.pageSize = pageData.pageSize || pagination.value.pageSize;
        hasMore.value = pageData.currentPage + 1 < pageData.totalPages;
        artifactStore.setArtifactList(artifactList.value);
        artifactStore.setPagination(
          pagination.value.page,
          pagination.value.pageSize,
          pagination.value.total
        );
      } catch (error) {
        console.error("加载文物列表失败:", error);
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
      pagination.value.page = 0;
      loadArtifacts(true);
    };
    const applyFilter = () => {
      showFilter.value = false;
      pagination.value.page = 0;
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
        a: common_vendor.o(handleSearch, "25"),
        b: searchKeyword.value,
        c: common_vendor.o(($event) => searchKeyword.value = $event.detail.value, "b1"),
        d: common_vendor.o(($event) => showFilter.value = true, "1f"),
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
        k: common_vendor.o(loadMore, "d5"),
        l: refreshing.value,
        m: common_vendor.o(onRefresh, "e1"),
        n: common_vendor.o(($event) => showFilter.value = false, "b6"),
        o: filter.value.period === "" ? 1 : "",
        p: common_vendor.o(($event) => filter.value.period = "", "03"),
        q: filter.value.period === "商周" ? 1 : "",
        r: common_vendor.o(($event) => filter.value.period = "商周", "44"),
        s: filter.value.period === "秦汉" ? 1 : "",
        t: common_vendor.o(($event) => filter.value.period = "秦汉", "fd"),
        v: filter.value.period === "隋唐" ? 1 : "",
        w: common_vendor.o(($event) => filter.value.period = "隋唐", "4e"),
        x: filter.value.period === "宋元" ? 1 : "",
        y: common_vendor.o(($event) => filter.value.period = "宋元", "9f"),
        z: filter.value.period === "明清" ? 1 : "",
        A: common_vendor.o(($event) => filter.value.period = "明清", "1d"),
        B: filter.value.material === "" ? 1 : "",
        C: common_vendor.o(($event) => filter.value.material = "", "eb"),
        D: filter.value.material === "青铜器" ? 1 : "",
        E: common_vendor.o(($event) => filter.value.material = "青铜器", "3f"),
        F: filter.value.material === "陶瓷" ? 1 : "",
        G: common_vendor.o(($event) => filter.value.material = "陶瓷", "27"),
        H: filter.value.material === "玉器" ? 1 : "",
        I: common_vendor.o(($event) => filter.value.material = "玉器", "94"),
        J: filter.value.material === "金银器" ? 1 : "",
        K: common_vendor.o(($event) => filter.value.material = "金银器", "7b"),
        L: common_vendor.o(resetFilter, "15"),
        M: common_vendor.o(applyFilter, "8b"),
        N: common_vendor.o(() => {
        }, "4e"),
        O: showFilter.value ? 1 : "",
        P: common_vendor.o(($event) => showFilter.value = false, "b8"),
        Q: common_vendor.p({
          visible: loading.value && artifactList.value.length === 0,
          text: "加载中..."
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-511649f2"]]);
wx.createPage(MiniProgramPage);
