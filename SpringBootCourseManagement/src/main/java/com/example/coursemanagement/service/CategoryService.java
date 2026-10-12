package com.example.coursemanagement.service;

import com.example.coursemanagement.dto.CategoryForm;
import com.example.coursemanagement.model.CourseCategory;
import com.example.coursemanagement.repository.CourseCategoryRepository;
import com.example.coursemanagement.repository.CourseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CourseCategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public CategoryService(CourseCategoryRepository categoryRepository, CourseRepository courseRepository) {
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseCategory> findAll() {
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public boolean exists(Integer id) {
        return id != null && categoryRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public CourseCategory get(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loại khóa học có mã " + id + " không tồn tại."));
    }

    @Transactional(readOnly = true)
    public boolean isNameTaken(String name, Integer excludeId) {
        return excludeId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    @Transactional
    public CourseCategory create(CategoryForm form) {
        CourseCategory category = new CourseCategory(form.getName(), form.getDescription());
        return categoryRepository.save(category);
    }

    @Transactional
    public CourseCategory update(Integer id, CategoryForm form) {
        CourseCategory category = get(id);
        category.setName(form.getName());
        category.setDescription(form.getDescription());
        return category;
    }

    /** Ràng buộc 1: không cho xóa loại khóa học nếu vẫn còn khóa học thuộc loại đó. */
    @Transactional
    public String delete(Integer id) {
        CourseCategory category = get(id);
        long courseCount = courseRepository.countByCategoryId(id);
        if (courseCount > 0) {
            throw new BusinessRuleException("Không thể xóa loại khóa học \"" + category.getName()
                    + "\" vì vẫn còn " + courseCount + " khóa học thuộc loại này.");
        }
        categoryRepository.delete(category);
        return category.getName();
    }
}
