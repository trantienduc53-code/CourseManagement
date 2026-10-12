package com.example.coursemanagement.dto;

import com.example.coursemanagement.model.Course;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Dữ liệu form Create / Edit khóa học (kèm validation). */
public class CourseForm {

    @NotBlank(message = "Tên khóa học không được để trống.")
    @Size(max = 200, message = "Tên khóa học tối đa 200 ký tự.")
    private String name;

    @NotNull(message = "Học phí không được để trống.")
    @DecimalMin(value = "0", inclusive = false, message = "Học phí phải lớn hơn 0.")
    @DecimalMax(value = "1000000000000", message = "Học phí quá lớn.")
    private BigDecimal tuitionFee;

    @NotNull(message = "Thời lượng không được để trống.")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0.")
    private Integer duration;

    private boolean active = true;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự.")
    private String description;

    @NotNull(message = "Vui lòng chọn loại khóa học.")
    private Integer courseCategoryId;

    public static CourseForm from(Course course) {
        CourseForm form = new CourseForm();
        form.setName(course.getName());
        form.setTuitionFee(course.getTuitionFee());
        form.setDuration(course.getDuration());
        form.setActive(course.isActive());
        form.setDescription(course.getDescription());
        form.setCourseCategoryId(course.getCategory().getId());
        return form;
    }

    public String getName() { return name; }
    /** Tự cắt khoảng trắng hai đầu để "   " bị coi là rỗng. */
    public void setName(String name) { this.name = name == null ? null : name.trim(); }

    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCourseCategoryId() { return courseCategoryId; }
    public void setCourseCategoryId(Integer courseCategoryId) { this.courseCategoryId = courseCategoryId; }
}
