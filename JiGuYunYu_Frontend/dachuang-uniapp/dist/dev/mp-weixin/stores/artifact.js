"use strict";
const common_vendor = require("../common/vendor.js");
const useArtifactStore = common_vendor.defineStore("artifact", {
  state: () => ({
    artifactList: [],
    currentArtifact: null,
    pagination: {
      page: 1,
      pageSize: 20,
      total: 0
    }
  }),
  getters: {
    hasArtifacts: (state) => state.artifactList.length > 0
  },
  actions: {
    setArtifactList(list) {
      this.artifactList = list;
    },
    setCurrentArtifact(artifact) {
      this.currentArtifact = artifact;
    },
    setPagination(page, pageSize, total) {
      this.pagination = { page, pageSize, total };
    },
    reset() {
      this.artifactList = [];
      this.currentArtifact = null;
      this.pagination = {
        page: 1,
        pageSize: 20,
        total: 0
      };
    }
  }
});
exports.useArtifactStore = useArtifactStore;
