package com.example.coursemanagement.web;

import com.example.coursemanagement.dto.CategoryForm;
import com.example.coursemanagement.service.BusinessRuleException;
import com.example.coursemanagement.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "categories/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new CategoryForm());
        return formView(model, "Thêm loại khóa học", "/categories/create");
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        checkDuplicate(form, null, binding);
        if (binding.hasErrors()) {
            return formView(model, "Thêm loại khóa học", "/categories/create");
        }
        categoryService.create(form);
        redirect.addFlashAttribute("success", "Đã thêm loại khóa học \"" + form.getName() + "\".");
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("form", CategoryForm.from(categoryService.get(id)));
        return formView(model, "Sửa loại khóa học", "/categories/" + id + "/edit");
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, @Valid @ModelAttribute("form") CategoryForm form,
                       BindingResult binding, Model model, RedirectAttributes redirect) {
        categoryService.get(id); // CourseCategoryId không tồn tại -> trang 404 thân thiện
        checkDuplicate(form, id, binding);
        if (binding.hasErrors()) {
            return formView(model, "Sửa loại khóa học", "/categories/" + id + "/edit");
        }
        categoryService.update(id, form);
        redirect.addFlashAttribute("success", "Đã cập nhật loại khóa học \"" + form.getName() + "\".");
        return "redirect:/categories";
    }

    // Ràng buộc 1: không cho xóa loại khóa học nếu vẫn còn khóa học thuộc loại đó
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String name = categoryService.delete(id);
            redirect.addFlashAttribute("success", "Đã xóa loại khóa học \"" + name + "\".");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    private void checkDuplicate(CategoryForm form, Integer excludeId, BindingResult binding) {
        if (form.getName() != null && !form.getName().isBlank()
                && categoryService.isNameTaken(form.getName(), excludeId)) {
            binding.rejectValue("name", "name.duplicate", "Tên loại khóa học đã tồn tại.");
        }
    }

    private String formView(Model model, String title, String action) {
        model.addAttribute("title", title);
        model.addAttribute("formAction", action);
        return "categories/form";
    }
}
