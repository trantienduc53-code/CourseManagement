package com.example.coursemanagement.dto;

import java.math.BigDecimal;

/** Các điều kiện tìm kiếm / lọc / sắp xếp trên trang danh sách (nhận từ query string). */
public class CourseFilter {

    private String keyword;
    private Integer categoryId;
    /** "" = tất cả, "active" = Đang tuyển sinh, "inactive" = Ngừng tuyển sinh */
    private String status;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    /** fee_asc | fee_desc | duration_asc */
    private String sort;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getMinFee() { return minFee; }
    public void setMinFee(BigDecimal minFee) { this.minFee = minFee; }

    public BigDecimal getMaxFee() { return maxFee; }
    public void setMaxFee(BigDecimal maxFee) { this.maxFee = maxFee; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
