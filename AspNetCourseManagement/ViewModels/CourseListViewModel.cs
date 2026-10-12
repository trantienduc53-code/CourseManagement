using CourseManagement.Models;

namespace CourseManagement.ViewModels;

/// <summary>Các điều kiện tìm kiếm / lọc / sắp xếp (nhận từ query string).</summary>
public class CourseFilterViewModel
{
    public string? Keyword { get; set; }
    public int? CategoryId { get; set; }

    /// <summary>"" = tất cả, "active" = Đang tuyển sinh, "inactive" = Ngừng tuyển sinh</summary>
    public string? Status { get; set; }

    public decimal? MinFee { get; set; }
    public decimal? MaxFee { get; set; }

    /// <summary>fee_asc | fee_desc | duration_asc</summary>
    public string? Sort { get; set; }
}

public class CourseListViewModel
{
    public CourseFilterViewModel Filter { get; set; } = new();
    public List<Course> Courses { get; set; } = new();
    public List<CourseCategory> Categories { get; set; } = new();
    public string? ErrorMessage { get; set; }
}
