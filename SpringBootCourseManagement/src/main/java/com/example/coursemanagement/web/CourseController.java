package com.example.coursemanagement.web;

import com.example.coursemanagement.dto.CourseFilter;
import com.example.coursemanagement.dto.CourseForm;
import com.example.coursemanagement.model.Course;
import com.example.coursemanagement.service.BusinessRuleException;
import com.example.coursemanagement.service.CategoryService;
import com.example.coursemanagement.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    public CourseController(CourseService courseService, CategoryService categoryService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
    }

    // ---------------------------------------------------------------
    // Chức năng 2 + 3: danh sách, tìm kiếm, lọc, sắp xếp
    // ---------------------------------------------------------------
    @GetMapping
    public String list(@ModelAttribute("filter") CourseFilter filter, BindingResult binding, Model model) {
        String errorMessage = null;

        // Giá trị nhập sai kiểu (ví dụ chữ trong ô học phí) -> báo nhẹ nhàng, bỏ qua giá trị đó
        if (binding.hasErrors()) {
            errorMessage = "Một số điều kiện lọc không hợp lệ nên đã được bỏ qua. Vui lòng kiểm tra lại.";
        }

        BigDecimal minFee = filter.getMinFee();
        BigDecimal maxFee = filter.getMaxFee();
        if (minFee != null && maxFee != null && minFee.compareTo(maxFee) > 0) {
            errorMessage = "Học phí tối thiểu không được lớn hơn học phí tối đa.";
            minFee = null;
            maxFee = null;
        }

        List<Course> courses = courseService.search(filter.getKeyword(), filter.getCategoryId(),
                filter.getStatus(), minFee, maxFee, filter.getSort());

        model.addAttribute("courses", courses);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("errorMessage", errorMessage);
        return "courses/list";
    }

    // ---------------------------------------------------------------
    // Detail: hiển thị tất cả ảnh của khóa học
    // ---------------------------------------------------------------
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("course", courseService.getDetail(id));
        return "courses/detail";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new CourseForm());
        return formView(model, "Thêm khóa học", "/courses/create");
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") CourseForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        checkBusinessRules(form, null, binding);
        if (binding.hasErrors()) {
            return formView(model, "Thêm khóa học", "/courses/create");
        }
        try {
            Course course = courseService.create(form);
            redirect.addFlashAttribute("success", "Đã thêm khóa học \"" + course.getName() + "\".");
            return "redirect:/courses";
        } catch (DataIntegrityViolationException e) {
            binding.reject("save.failed", "Không thể lưu khóa học. Có thể tên khóa học đã bị trùng, vui lòng thử lại.");
            return formView(model, "Thêm khóa học", "/courses/create");
        }
    }

    // ---------------------------------------------------------------
    // Edit
    // ---------------------------------------------------------------
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("form", courseService.getFormForEdit(id));
        return formView(model, "Sửa khóa học", "/courses/" + id + "/edit");
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, @Valid @ModelAttribute("form") CourseForm form,
                       BindingResult binding, Model model, RedirectAttributes redirect) {
        courseService.requireExists(id); // CourseId không tồn tại -> trang 404 thân thiện
        checkBusinessRules(form, id, binding);
        if (binding.hasErrors()) {
            return formView(model, "Sửa khóa học", "/courses/" + id + "/edit");
        }
        try {
            Course course = courseService.update(id, form);
            redirect.addFlashAttribute("success", "Đã cập nhật khóa học \"" + course.getName() + "\".");
            return "redirect:/courses";
        } catch (DataIntegrityViolationException e) {
            binding.reject("save.failed", "Không thể cập nhật khóa học. Có thể tên khóa học đã bị trùng.");
            return formView(model, "Sửa khóa học", "/courses/" + id + "/edit");
        }
    }

    // ---------------------------------------------------------------
    // Delete khóa học
    // ---------------------------------------------------------------
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        String name = courseService.delete(id);
        redirect.addFlashAttribute("success", "Đã xóa khóa học \"" + name + "\".");
        return "redirect:/courses";
    }

    // ---------------------------------------------------------------
    // Chức năng 4: quản lý ảnh khóa học
    // ---------------------------------------------------------------
    @PostMapping("/{id}/images")
    public String addImage(@PathVariable Integer id,
                           @RequestParam(required = false) String imageUrl,
                           @RequestParam(defaultValue = "false") boolean thumbnail,
                           RedirectAttributes redirect) {
        try {
            courseService.addImage(id, imageUrl, thumbnail);
            redirect.addFlashAttribute("success", "Đã thêm ảnh cho khóa học.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/images/{imageId}/thumbnail")
    public String setThumbnail(@PathVariable Integer imageId, RedirectAttributes redirect) {
        Integer courseId = courseService.setThumbnail(imageId);
        redirect.addFlashAttribute("success", "Đã đổi ảnh đại diện.");
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/images/{imageId}/delete")
    public String deleteImage(@PathVariable Integer imageId, RedirectAttributes redirect) {
        Integer courseId = courseService.deleteImage(imageId);
        redirect.addFlashAttribute("success", "Đã xóa ảnh.");
        return "redirect:/courses/" + courseId;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** CourseCategoryId phải tồn tại; tên khóa học không được trùng trong cùng loại. */
    private void checkBusinessRules(CourseForm form, Integer excludeId, BindingResult binding) {
        Integer categoryId = form.getCourseCategoryId();
        if (categoryId == null) {
            return; // lỗi "Vui lòng chọn loại khóa học" đã do validation báo
        }
        if (!categoryService.exists(categoryId)) {
            binding.rejectValue("courseCategoryId", "category.notfound", "Loại khóa học không tồn tại.");
            return;
        }
        if (form.getName() != null && !form.getName().isBlank()
                && courseService.isNameTaken(categoryId, form.getName(), excludeId)) {
            binding.rejectValue("name", "name.duplicate", "Tên khóa học đã tồn tại trong loại khóa học này.");
        }
    }

    /** Nạp danh sách loại khóa học từ database cho ô chọn ở trang Create/Edit. */
    private String formView(Model model, String title, String action) {
        model.addAttribute("title", title);
        model.addAttribute("formAction", action);
        model.addAttribute("categories", categoryService.findAll());
        return "courses/form";
    }
}
