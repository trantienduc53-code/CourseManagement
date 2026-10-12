package com.example.coursemanagement.service;

/** Không tìm thấy dữ liệu (CourseId / CourseCategoryId / CourseImageId không tồn tại...). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
