package com.example.coursemanagement.config;

import com.example.coursemanagement.model.Course;
import com.example.coursemanagement.model.CourseCategory;
import com.example.coursemanagement.model.CourseImage;
import com.example.coursemanagement.repository.CourseCategoryRepository;
import com.example.coursemanagement.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Nạp dữ liệu mẫu (3 loại khóa học, 6 khóa học, nhiều ảnh) khi database còn trống. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CourseCategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public DataSeeder(CourseCategoryRepository categoryRepository, CourseRepository courseRepository) {
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        CourseCategory web = categoryRepository.save(new CourseCategory(
                "Lập trình Web", "Các khóa học xây dựng website và ứng dụng web."));
        CourseCategory mobile = categoryRepository.save(new CourseCategory(
                "Lập trình Di động", "Các khóa học phát triển ứng dụng di động."));
        CourseCategory data = categoryRepository.save(new CourseCategory(
                "Cơ sở dữ liệu", "Các khóa học về cơ sở dữ liệu và phân tích dữ liệu."));

        Course aspnet = course("ASP.NET Core", 4_500_000, 40, true,
                "Xây dựng ứng dụng web với ASP.NET Core MVC và Entity Framework Core.", web);
        image(aspnet, "aspnet-1", true);
        image(aspnet, "aspnet-2", false);
        image(aspnet, "aspnet-3", false);

        Course react = course("ReactJS", 3_200_000, 24, true,
                "Xây dựng giao diện web hiện đại với ReactJS.", web);
        image(react, "react-1", true);
        image(react, "react-2", false);

        Course html = course("HTML/CSS/JavaScript nhập môn", 1_800_000, 20, true,
                "Nền tảng frontend cho người mới bắt đầu.", web);
        image(html, "html-1", true);

        Course flutter = course("Flutter cho người mới", 3_900_000, 36, true,
                "Lập trình ứng dụng đa nền tảng với Flutter.", mobile);
        image(flutter, "flutter-1", true);

        Course sql = course("SQL Server căn bản", 2_500_000, 30, true,
                "Thiết kế và truy vấn cơ sở dữ liệu với SQL Server.", data);
        image(sql, "sql-1", true);
        image(sql, "sql-2", false);

        // Cố ý không có ảnh để demo trường hợp "Khóa học chưa có ảnh", và đang ngừng tuyển sinh
        Course powerBi = course("Power BI phân tích dữ liệu", 2_800_000, 28, false,
                "Trực quan hóa dữ liệu với Power BI (tạm ngừng tuyển sinh).", data);

        courseRepository.save(aspnet);
        courseRepository.save(react);
        courseRepository.save(html);
        courseRepository.save(flutter);
        courseRepository.save(sql);
        courseRepository.save(powerBi);
    }

    private Course course(String name, long fee, int duration, boolean active, String description,
                          CourseCategory category) {
        Course c = new Course();
        c.setName(name);
        c.setTuitionFee(BigDecimal.valueOf(fee));
        c.setDuration(duration);
        c.setActive(active);
        c.setDescription(description);
        c.setCategory(category);
        return c;
    }

    private void image(Course course, String seed, boolean thumbnail) {
        course.getImages().add(new CourseImage(course, "https://picsum.photos/seed/" + seed + "/600/400", thumbnail));
    }
}
