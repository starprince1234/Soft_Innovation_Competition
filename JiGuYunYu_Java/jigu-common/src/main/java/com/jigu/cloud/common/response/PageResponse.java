package com.jigu.cloud.common.response;

import java.util.List;

/**
 * 统一分页响应 data 结构。
 * <p>
 * 与文档分页规范对齐：totalElements / totalPages / currentPage / pageSize / list。
 *
 * @param <T> 列表元素类型
 */
public class PageResponse<T> {

    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private List<T> list;

    public PageResponse() {
    }

    public PageResponse(long totalElements, int totalPages, int currentPage, int pageSize, List<T> list) {
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.list = list;
    }

    /**
     * 从 Spring Data Page 对象快速构建。
     */
    public static <T> PageResponse<T> of(List<T> list, long totalElements, int currentPage, int pageSize) {
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        return new PageResponse<>(totalElements, totalPages, currentPage, pageSize, list);
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
