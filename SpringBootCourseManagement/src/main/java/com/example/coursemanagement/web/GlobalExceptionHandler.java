package com.example.coursemanagement.web;

import com.example.coursemanagement.service.NotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Bắt mọi lỗi và hiển thị trang thân thiện, không để lỗi hệ thống (stack trace) hiện ra màn hình.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** CourseId / CourseCategoryId / CourseImageId không tồn tại. */
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(NotFoundException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }

    /** Mã không phải số, ví dụ /courses/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleBadId(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("message", "Mã bạn truy cập không hợp lệ hoặc không tồn tại.");
        return "error/not-found";
    }

    /** Đường dẫn không tồn tại. */
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("message", "Trang bạn truy cập không tồn tại.");
        return "error/not-found";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        model.addAttribute("message", "Yêu cầu không được hỗ trợ cho trang này.");
        return "error/not-found";
    }

    /** Mọi lỗi còn lại. */
    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex, HttpServletResponse response) {
        log.error("Lỗi không lường trước", ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return "error";
    }
}
