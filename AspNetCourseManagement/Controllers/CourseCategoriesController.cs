using CourseManagement.Data;
using CourseManagement.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CourseManagement.Controllers;

public class CourseCategoriesController : AppController
{
    private readonly AppDbContext _db;

    public CourseCategoriesController(AppDbContext db)
    {
        _db = db;
    }

    public async Task<IActionResult> Index()
    {
        var categories = await _db.CourseCategories
            .AsNoTracking()
            .Include(c => c.Courses)
            .OrderBy(c => c.Name)
            .ToListAsync();
        return View(categories);
    }

    public IActionResult Create() => View(new CourseCategory());

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create([Bind("Name,Description")] CourseCategory category)
    {
        category.Name = category.Name?.Trim() ?? string.Empty;
        await CheckDuplicateNameAsync(category);

        if (!ModelState.IsValid)
        {
            return View(category);
        }

        _db.CourseCategories.Add(category);
        await _db.SaveChangesAsync();
        TempData["Success"] = $"Đã thêm loại khóa học \"{category.Name}\".";
        return RedirectToAction(nameof(Index));
    }

    public async Task<IActionResult> Edit(int? id)
    {
        if (id == null)
        {
            return NotFoundPage("Không tìm thấy mã loại khóa học.");
        }

        var category = await _db.CourseCategories.AsNoTracking().FirstOrDefaultAsync(c => c.Id == id);
        if (category == null)
        {
            return NotFoundPage($"Loại khóa học có mã {id} không tồn tại.");
        }
        return View(category);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, [Bind("Id,Name,Description")] CourseCategory input)
    {
        if (id != input.Id)
        {
            return NotFoundPage("Mã loại khóa học không khớp.");
        }

        var category = await _db.CourseCategories.FirstOrDefaultAsync(c => c.Id == id);
        if (category == null)
        {
            return NotFoundPage($"Loại khóa học có mã {id} không tồn tại.");
        }

        input.Name = input.Name?.Trim() ?? string.Empty;
        await CheckDuplicateNameAsync(input);

        if (!ModelState.IsValid)
        {
            return View(input);
        }

        category.Name = input.Name;
        category.Description = input.Description;
        await _db.SaveChangesAsync();

        TempData["Success"] = $"Đã cập nhật loại khóa học \"{category.Name}\".";
        return RedirectToAction(nameof(Index));
    }

    // Ràng buộc 1: không cho xóa loại khóa học nếu vẫn còn khóa học thuộc loại đó
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Delete(int id)
    {
        var category = await _db.CourseCategories.FirstOrDefaultAsync(c => c.Id == id);
        if (category == null)
        {
            return NotFoundPage($"Loại khóa học có mã {id} không tồn tại.");
        }

        var courseCount = await _db.Courses.CountAsync(c => c.CourseCategoryId == id);
        if (courseCount > 0)
        {
            TempData["Error"] = $"Không thể xóa loại khóa học \"{category.Name}\" vì vẫn còn {courseCount} khóa học thuộc loại này.";
            return RedirectToAction(nameof(Index));
        }

        try
        {
            _db.CourseCategories.Remove(category);
            await _db.SaveChangesAsync();
            TempData["Success"] = $"Đã xóa loại khóa học \"{category.Name}\".";
        }
        catch (DbUpdateException)
        {
            // Phòng khi có khóa học được thêm vào đúng lúc đang xóa (FK Restrict ở database)
            TempData["Error"] = $"Không thể xóa loại khóa học \"{category.Name}\" vì đang được sử dụng.";
        }

        return RedirectToAction(nameof(Index));
    }

    private async Task CheckDuplicateNameAsync(CourseCategory category)
    {
        if (string.IsNullOrWhiteSpace(category.Name)) return;

        var duplicated = await _db.CourseCategories.AnyAsync(c => c.Name == category.Name && c.Id != category.Id);
        if (duplicated)
        {
            ModelState.AddModelError(nameof(CourseCategory.Name), "Tên loại khóa học đã tồn tại.");
        }
    }
}
