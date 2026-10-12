package com.example.coursemanagement.service;

import com.example.coursemanagement.dto.CourseForm;
import com.example.coursemanagement.model.Course;
import com.example.coursemanagement.model.CourseCategory;
import com.example.coursemanagement.model.CourseImage;
import com.example.coursemanagement.repository.CourseCategoryRepository;
import com.example.coursemanagement.repository.CourseImageRepository;
import com.example.coursemanagement.repository.CourseRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private static final String INVALID_URL_MESSAGE =
            "Đường dẫn ảnh không hợp lệ. Vui lòng nhập URL bắt đầu bằng http:// hoặc https:// (tối đa 500 ký tự).";

    private final CourseRepository courseRepository;
    private final CourseCategoryRepository categoryRepository;
    private final CourseImageRepository imageRepository;

    public CourseService(CourseRepository courseRepository,
                         CourseCategoryRepository categoryRepository,
                         CourseImageRepository imageRepository) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
    }

    // ------------------------------------------------------------------
    // Chức năng 2: tìm kiếm + lọc + sắp xếp
    // Tất cả điều kiện được dịch thành MỘT câu SQL (JPA Specification),
    // không lấy toàn bộ dữ liệu rồi lọc bằng vòng lặp.
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Course> search(String keyword, Integer categoryId, String status,
                               BigDecimal minFee, BigDecimal maxFee, String sort) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern, '\\'));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            // Ràng buộc 3: "Đang tuyển sinh" chỉ hiển thị IsActive = true
            if ("active".equals(status)) {
                predicates.add(cb.isTrue(root.get("active")));
            } else if ("inactive".equals(status)) {
                predicates.add(cb.isFalse(root.get("active")));
            }
            if (minFee != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("tuitionFee"), minFee));
            }
            if (maxFee != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("tuitionFee"), maxFee));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return courseRepository.findAll(spec, toSort(sort));
    }

    private static Sort toSort(String sort) {
        String key = sort == null ? "" : sort;
        return switch (key) {
            case "fee_asc" -> Sort.by(Sort.Order.asc("tuitionFee"), Sort.Order.asc("id"));
            case "fee_desc" -> Sort.by(Sort.Order.desc("tuitionFee"), Sort.Order.asc("id"));
            case "duration_asc" -> Sort.by(Sort.Order.asc("duration"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.asc("id"));
        };
    }

    /** Thoát các ký tự đặc biệt của LIKE (% _ \) để tìm đúng chuỗi người dùng nhập. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ------------------------------------------------------------------
    // Đọc dữ liệu
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Course getDetail(Integer id) {
        return courseRepository.findWithDetailsById(id)
                .orElseThrow(() -> courseNotFound(id));
    }

    @Transactional(readOnly = true)
    public CourseForm getFormForEdit(Integer id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> courseNotFound(id));
        return CourseForm.from(course);
    }

    @Transactional(readOnly = true)
    public void requireExists(Integer id) {
        if (!courseRepository.existsById(id)) {
            throw courseNotFound(id);
        }
    }

    /** Tên khóa học đã tồn tại trong cùng loại khóa học chưa (không tính chính khóa học đang sửa). */
    @Transactional(readOnly = true)
    public boolean isNameTaken(Integer categoryId, String name, Integer excludeId) {
        return excludeId == null
                ? courseRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, name)
                : courseRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, name, excludeId);
    }

    // ------------------------------------------------------------------
    // Create / Edit / Delete
    // ------------------------------------------------------------------
    @Transactional
    public Course create(CourseForm form) {
        Course course = new Course();
        apply(course, form);
        return courseRepository.save(course);
    }

    @Transactional
    public Course update(Integer id, CourseForm form) {
        Course course = courseRepository.findById(id).orElseThrow(() -> courseNotFound(id));
        apply(course, form);
        return courseRepository.saveAndFlush(course);
    }

    @Transactional
    public String delete(Integer id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> courseNotFound(id));
        courseRepository.delete(course); // ảnh bị xóa theo (cascade)
        return course.getName();
    }

    private void apply(Course course, CourseForm form) {
        CourseCategory category = categoryRepository.findById(form.getCourseCategoryId())
                .orElseThrow(() -> new NotFoundException(
                        "Loại khóa học có mã " + form.getCourseCategoryId() + " không tồn tại."));
        course.setName(form.getName());
        course.setTuitionFee(form.getTuitionFee());
        course.setDuration(form.getDuration());
        course.setActive(form.isActive());
        course.setDescription(form.getDescription());
        course.setCategory(category);
    }

    // ------------------------------------------------------------------
    // Chức năng 4: quản lý ảnh
    // ------------------------------------------------------------------
    @Transactional
    public void addImage(Integer courseId, String rawUrl, boolean makeThumbnail) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> courseNotFound(courseId));
        String url = normalizeImageUrl(rawUrl);

        CourseImage image = imageRepository.save(new CourseImage(course, url, false));

        // Đặt làm ảnh đại diện nếu người dùng chọn, hoặc nếu khóa học chưa có ảnh đại diện nào
        boolean alreadyHasThumbnail = imageRepository.existsByCourseIdAndThumbnailTrue(courseId);
        if (makeThumbnail || !alreadyHasThumbnail) {
            applyThumbnail(courseId, image.getId());
        }
    }

    /** Chọn / đổi ảnh đại diện. Trả về CourseId để controller quay lại trang Detail. */
    @Transactional
    public Integer setThumbnail(Integer imageId) {
        CourseImage image = imageRepository.findById(imageId).orElseThrow(() -> imageNotFound(imageId));
        Integer courseId = image.getCourse().getId();
        applyThumbnail(courseId, imageId);
        return courseId;
    }

    @Transactional
    public Integer deleteImage(Integer imageId) {
        CourseImage image = imageRepository.findById(imageId).orElseThrow(() -> imageNotFound(imageId));
        Integer courseId = image.getCourse().getId();
        imageRepository.delete(image);
        return courseId;
    }

    /** Ảnh đại diện cũ tự động chuyển thành ảnh thường, rồi bật cờ cho ảnh mới => luôn tối đa 1 ảnh đại diện. */
    private void applyThumbnail(Integer courseId, Integer imageId) {
        imageRepository.clearOtherThumbnails(courseId, imageId);
        imageRepository.markAsThumbnail(imageId);
    }

    private static String normalizeImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessRuleException(INVALID_URL_MESSAGE);
        }
        String url = rawUrl.trim();
        if (url.length() > 500) {
            throw new BusinessRuleException(INVALID_URL_MESSAGE);
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null) {
                throw new BusinessRuleException(INVALID_URL_MESSAGE);
            }
        } catch (URISyntaxException e) {
            throw new BusinessRuleException(INVALID_URL_MESSAGE);
        }
        return url;
    }

    private static NotFoundException courseNotFound(Integer id) {
        return new NotFoundException("Khóa học có mã " + id + " không tồn tại.");
    }

    private static NotFoundException imageNotFound(Integer id) {
        return new NotFoundException("Ảnh có mã " + id + " không tồn tại.");
    }
}
