package com.example.coursemanagement.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Courses",
        // Tên khóa học không được trùng trong cùng một loại khóa học
        uniqueConstraints = @UniqueConstraint(name = "UX_Courses_Category_Name",
                columnNames = {"CourseCategoryId", "Name"}))
public class Course {

    /** Ngưỡng phân loại thời lượng: <= 30 giờ là Ngắn hạn, ngược lại là Dài hạn. */
    public static final int SHORT_TERM_MAX_HOURS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 200)
    private String name;

    @Column(name = "TuitionFee", nullable = false, precision = 18, scale = 2)
    private BigDecimal tuitionFee;

    @Column(name = "Duration", nullable = false)
    private Integer duration;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;

    @Column(name = "Description", length = 2000)
    private String description;

    // 1 CourseCategory có nhiều Course  (khóa ngoại CourseCategoryId)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseCategoryId", nullable = false)
    private CourseCategory category;

    // 1 Course có nhiều CourseImage
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("thumbnail DESC, id ASC")
    @BatchSize(size = 50)
    private List<CourseImage> images = new ArrayList<>();

    public Course() {
    }

    // ---- Các giá trị chỉ tính khi hiển thị, KHÔNG có cột trong database ----

    /** Chức năng 3: Ngắn hạn (Duration <= 30) / Dài hạn (Duration > 30). */
    @Transient
    public String getDurationType() {
        return duration != null && duration <= SHORT_TERM_MAX_HOURS ? "Ngắn hạn" : "Dài hạn";
    }

    /** Ràng buộc 2: IsActive = false => "Ngừng tuyển sinh". */
    @Transient
    public String getStatusText() {
        return active ? "Đang tuyển sinh" : "Ngừng tuyển sinh";
    }

    /** Ảnh đại diện (null nếu khóa học chưa có ảnh đại diện). */
    @Transient
    public CourseImage getThumbnailImage() {
        return images.stream().filter(CourseImage::isThumbnail).findFirst().orElse(null);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CourseCategory getCategory() { return category; }
    public void setCategory(CourseCategory category) { this.category = category; }

    public List<CourseImage> getImages() { return images; }
    public void setImages(List<CourseImage> images) { this.images = images; }
}
