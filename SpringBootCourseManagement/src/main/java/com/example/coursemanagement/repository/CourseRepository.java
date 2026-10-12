package com.example.coursemanagement.repository;

import com.example.coursemanagement.model.Course;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer>, JpaSpecificationExecutor<Course> {

    /** Tìm kiếm / lọc động: lấy luôn loại khóa học trong cùng một câu SQL (tránh N+1). */
    @Override
    @EntityGraph(attributePaths = "category")
    List<Course> findAll(Specification<Course> spec, Sort sort);

    /** Trang Detail: lấy khóa học kèm loại khóa học và danh sách ảnh. */
    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Course> findWithDetailsById(Integer id);

    long countByCategoryId(Integer categoryId);

    boolean existsByCategoryIdAndNameIgnoreCase(Integer categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Integer categoryId, String name, Integer id);
}
