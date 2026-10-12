package com.example.coursemanagement.service;

/** Vi phạm ràng buộc nghiệp vụ (ví dụ xóa loại khóa học còn khóa học, URL ảnh sai...). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
