using CourseManagement.Data;
using CourseManagement.Models;
using CourseManagement.ViewModels;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;

namespace CourseManagement.Controllers;

public class CoursesController : AppController
{
    private readonly AppDbContext _db;

    public CoursesController(AppDbContext db)
    {
        _db = db;
    }

    // ---------------------------------------------------------------
    // Chức năng 2 + 3: Danh sách, tìm kiếm, lọc, sắp xếp
    // ---------------------------------------------------------------
    public async Task<IActionResult> Index(CourseFilterViewModel filter)
    {
        var vm = new CourseListViewModel { Filter = filter };

        // Giá trị nhập sai kiểu (ví dụ chữ trong ô học phí) -> báo lỗi nhẹ nhàng, bỏ qua giá trị đó
        if (!ModelState.IsValid)
        {
            vm.ErrorMessage = "Một số điều kiện lọc không hợp lệ nên đã được bỏ qua. Vui lòng kiểm tra lại.";
            ModelState.Clear();
        }

        // Toàn bộ điều kiện được dịch thành câu SQL (IQueryable), KHÔNG lấy hết dữ liệu rồi lọc bằng vòng lặp
        IQueryable<Course> query = _db.Courses
            .AsNoTracking()
            .Include(c => c.CourseCategory)
            .Include(c => c.Images);

        if (!string.IsNullOrWhiteSpace(filter.Keyword))
        {
            var keyword = filter.Keyword.Trim();
            query = query.Where(c => c.Name.Contains(keyword));
        }

        if (filter.CategoryId.HasValue)
        {
            query = query.Where(c => c.CourseCategoryId == filter.CategoryId.Value);
        }

        // Ràng buộc 3: "Đang tuyển sinh" chỉ hiển thị IsActive = true
        if (filter.Status == "active")
        {
            query = query.Where(c => c.IsActive);
        }
        else if (filter.Status == "inactive")
        {
            query = query.Where(c => !c.IsActive);
        }

        // Học phí tối thiểu > tối đa -> báo lỗi và không áp dụng lọc theo học phí
        if (filter.MinFee.HasValue && filter.MaxFee.HasValue && filter.MinFee > filter.MaxFee)
        {
            vm.ErrorMessage = "Học phí tối thiểu không được lớn hơn học phí tối đa.";
        }
        else
        {
            if (filter.MinFee.HasValue)
            {
                query = query.Where(c => c.TuitionFee >= filter.MinFee.Value);
            }
            if (filter.MaxFee.HasValue)
            {
                query = query.Where(c => c.TuitionFee <= filter.MaxFee.Value);
            }
        }

        query = filter.Sort switch
        {
            "fee_asc" => query.OrderBy(c => c.TuitionFee).ThenBy(c => c.Id),
            "fee_desc" => query.OrderByDescending(c => c.TuitionFee).ThenBy(c => c.Id),
            "duration_asc" => query.OrderBy(c => c.Duration).ThenBy(c => c.Id),
            _ => query.OrderBy(c => c.Id)
        };

        vm.Courses = await query.ToListAsync();
        vm.Categories = await _db.CourseCategories.AsNoTracking().OrderBy(c => c.Name).ToListAsync();
        return View(vm);
    }

    // ---------------------------------------------------------------
    // Detail: hiển thị tất cả ảnh của khóa học
    // ---------------------------------------------------------------
    public async Task<IActionResult> Details(int? id)
    {
        if (id == null)
        {
            return NotFoundPage("Không tìm thấy mã khóa học.");
        }

        var course = await _db.Courses
            .AsNoTracking()
            .Include(c => c.CourseCategory)
            .Include(c => c.Images.OrderByDescending(i => i.IsThumbnail).ThenBy(i => i.Id))
            .FirstOrDefaultAsync(c => c.Id == id);

        if (course == null)
        {
            return NotFoundPage($"Khóa học có mã {id} không tồn tại.");
        }

        return View(course);
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------
    public async Task<IActionResult> Create()
    {
        await LoadCategoriesAsync(null);
        return View(new Course());
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(
        [Bind("Name,TuitionFee,Duration,IsActive,Description,CourseCategoryId")] Course course)
    {
        course.Name = course.Name?.Trim() ?? string.Empty;
        await ValidateBusinessRulesAsync(course);

        if (!ModelState.IsValid)
        {
            await LoadCategoriesAsync(course.CourseCategoryId);
            return View(course);
        }

        try
        {
            _db.Courses.Add(course);
            await _db.SaveChangesAsync();
        }
        catch (DbUpdateException)
        {
            ModelState.AddModelError(string.Empty, "Không thể lưu khóa học. Có thể tên khóa học đã bị trùng, vui lòng thử lại.");
            await LoadCategoriesAsync(course.CourseCategoryId);
            return View(course);
        }

        TempData["Success"] = $"Đã thêm khóa học \"{course.Name}\".";
        return RedirectToAction(nameof(Index));
    }

    // ---------------------------------------------------------------
    // Edit
    // ---------------------------------------------------------------
    public async Task<IActionResult> Edit(int? id)
    {
        if (id == null)
        {
            return NotFoundPage("Không tìm thấy mã khóa học.");
        }

        var course = await _db.Courses.AsNoTracking().FirstOrDefaultAsync(c => c.Id == id);
        if (course == null)
        {
            return NotFoundPage($"Khóa học có mã {id} không tồn tại.");
        }

        await LoadCategoriesAsync(course.CourseCategoryId);
        return View(course);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id,
        [Bind("Id,Name,TuitionFee,Duration,IsActive,Description,CourseCategoryId")] Course input)
    {
        if (id != input.Id)
        {
            return NotFoundPage("Mã khóa học không khớp.");
        }

        var course = await _db.Courses.FirstOrDefaultAsync(c => c.Id == id);
        if (course == null)
        {
            return NotFoundPage($"Khóa học có mã {id} không tồn tại.");
        }

        input.Name = input.Name?.Trim() ?? string.Empty;
        await ValidateBusinessRulesAsync(input);

        if (!ModelState.IsValid)
        {
            await LoadCategoriesAsync(input.CourseCategoryId);
            return View(input);
        }

        course.Name = input.Name;
        course.TuitionFee = input.TuitionFee;
        course.Duration = input.Duration;
        course.IsActive = input.IsActive;
        course.Description = input.Description;
        course.CourseCategoryId = input.CourseCategoryId;

        try
        {
            await _db.SaveChangesAsync();
        }
        catch (DbUpdateException)
        {
            ModelState.AddModelError(string.Empty, "Không thể cập nhật khóa học. Có thể tên khóa học đã bị trùng, hoặc dữ liệu đã bị thay đổi.");
            await LoadCategoriesAsync(input.CourseCategoryId);
            return View(input);
        }

        TempData["Success"] = $"Đã cập nhật khóa học \"{course.Name}\".";
        return RedirectToAction(nameof(Index));
    }

    // ---------------------------------------------------------------
    // Delete khóa học (ảnh bị xóa theo nhờ cascade)
    // ---------------------------------------------------------------
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Delete(int id)
    {
        var course = await _db.Courses.FirstOrDefaultAsync(c => c.Id == id);
        if (course == null)
        {
            return NotFoundPage($"Khóa học có mã {id} không tồn tại.");
        }

        _db.Courses.Remove(course);
        await _db.SaveChangesAsync();
        TempData["Success"] = $"Đã xóa khóa học \"{course.Name}\".";
        return RedirectToAction(nameof(Index));
    }

    // ---------------------------------------------------------------
    // Chức năng 4: Quản lý ảnh khóa học
    // ---------------------------------------------------------------
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> AddImage(int courseId, string? imageUrl, bool isThumbnail)
    {
        var course = await _db.Courses.Include(c => c.Images).FirstOrDefaultAsync(c => c.Id == courseId);
        if (course == null)
        {
            return NotFoundPage($"Khóa học có mã {courseId} không tồn tại.");
        }

        imageUrl = imageUrl?.Trim();
        if (string.IsNullOrEmpty(imageUrl)
            || imageUrl.Length > 500
            || !Uri.TryCreate(imageUrl, UriKind.Absolute, out var uri)
            || (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
        {
            TempData["Error"] = "Đường dẫn ảnh không hợp lệ. Vui lòng nhập URL bắt đầu bằng http:// hoặc https:// (tối đa 500 ký tự).";
            return RedirectToAction(nameof(Details), new { id = courseId });
        }

        var image = new CourseImage { CourseId = courseId, ImageUrl = imageUrl, IsThumbnail = false };
        _db.CourseImages.Add(image);
        await _db.SaveChangesAsync();

        // Đặt làm ảnh đại diện nếu người dùng chọn, hoặc nếu khóa học chưa có ảnh đại diện nào
        var hasThumbnail = course.Images.Any(i => i.IsThumbnail && i.Id != image.Id);
        if (isThumbnail || !hasThumbnail)
        {
            await SetThumbnailAsync(image);
        }

        TempData["Success"] = "Đã thêm ảnh cho khóa học.";
        return RedirectToAction(nameof(Details), new { id = courseId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> SetThumbnail(int imageId)
    {
        var image = await _db.CourseImages.AsNoTracking().FirstOrDefaultAsync(i => i.Id == imageId);
        if (image == null)
        {
            return NotFoundPage($"Ảnh có mã {imageId} không tồn tại.");
        }

        await SetThumbnailAsync(image);
        TempData["Success"] = "Đã đổi ảnh đại diện.";
        return RedirectToAction(nameof(Details), new { id = image.CourseId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteImage(int imageId)
    {
        var image = await _db.CourseImages.FirstOrDefaultAsync(i => i.Id == imageId);
        if (image == null)
        {
            return NotFoundPage($"Ảnh có mã {imageId} không tồn tại.");
        }

        var courseId = image.CourseId;
        _db.CourseImages.Remove(image);
        await _db.SaveChangesAsync();

        TempData["Success"] = "Đã xóa ảnh.";
        return RedirectToAction(nameof(Details), new { id = courseId });
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /// <summary>
    /// Chọn ảnh làm ảnh đại diện: ảnh đại diện cũ tự động chuyển thành ảnh thường.
    /// Làm trong một transaction; bỏ cờ của ảnh cũ TRƯỚC rồi mới bật cờ ảnh mới
    /// để không vi phạm unique index "tối đa 1 ảnh đại diện / khóa học".
    /// </summary>
    private async Task SetThumbnailAsync(CourseImage image)
    {
        await using var tx = await _db.Database.BeginTransactionAsync();

        await _db.CourseImages
            .Where(i => i.CourseId == image.CourseId && i.IsThumbnail && i.Id != image.Id)
            .ExecuteUpdateAsync(s => s.SetProperty(i => i.IsThumbnail, false));

        await _db.CourseImages
            .Where(i => i.Id == image.Id)
            .ExecuteUpdateAsync(s => s.SetProperty(i => i.IsThumbnail, true));

        await tx.CommitAsync();
    }

    /// <summary>Validation: CourseCategoryId phải tồn tại; tên không trùng trong cùng loại.</summary>
    private async Task ValidateBusinessRulesAsync(Course course)
    {
        if (course.CourseCategoryId <= 0)
        {
            return; // lỗi "Vui lòng chọn loại khóa học" đã do DataAnnotations báo
        }

        var categoryExists = await _db.CourseCategories.AnyAsync(c => c.Id == course.CourseCategoryId);
        if (!categoryExists)
        {
            ModelState.AddModelError(nameof(Course.CourseCategoryId), "Loại khóa học không tồn tại.");
            return;
        }

        if (!string.IsNullOrWhiteSpace(course.Name))
        {
            var duplicated = await _db.Courses.AnyAsync(c =>
                c.CourseCategoryId == course.CourseCategoryId &&
                c.Name == course.Name &&
                c.Id != course.Id);

            if (duplicated)
            {
                ModelState.AddModelError(nameof(Course.Name), "Tên khóa học đã tồn tại trong loại khóa học này.");
            }
        }
    }

    /// <summary>Nạp danh sách loại khóa học từ database cho ô chọn ở trang Create/Edit.</summary>
    private async Task LoadCategoriesAsync(int? selectedId)
    {
        var categories = await _db.CourseCategories.AsNoTracking().OrderBy(c => c.Name).ToListAsync();
        ViewBag.Categories = new SelectList(categories, nameof(CourseCategory.Id), nameof(CourseCategory.Name), selectedId);
    }
}
