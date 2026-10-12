using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CourseManagement.Models;

public class Course
{
    public int Id { get; set; }

    [Required(ErrorMessage = "Tên khóa học không được để trống.")]
    [StringLength(200, ErrorMessage = "Tên khóa học tối đa 200 ký tự.")]
    [Display(Name = "Tên khóa học")]
    public string Name { get; set; } = string.Empty;

    [Display(Name = "Học phí (VNĐ)")]
    [Range(typeof(decimal), "0.01", "1000000000000",
        ParseLimitsInInvariantCulture = true, ConvertValueInInvariantCulture = true,
        ErrorMessage = "Học phí phải lớn hơn 0.")]
    public decimal TuitionFee { get; set; }

    [Display(Name = "Thời lượng (giờ)")]
    [Range(1, int.MaxValue, ErrorMessage = "Thời lượng phải lớn hơn 0.")]
    public int Duration { get; set; }

    [Display(Name = "Đang tuyển sinh")]
    public bool IsActive { get; set; } = true;

    [StringLength(2000, ErrorMessage = "Mô tả tối đa 2000 ký tự.")]
    [Display(Name = "Mô tả")]
    public string? Description { get; set; }

    [Display(Name = "Loại khóa học")]
    [Range(1, int.MaxValue, ErrorMessage = "Vui lòng chọn loại khóa học.")]
    public int CourseCategoryId { get; set; }

    public CourseCategory? CourseCategory { get; set; }

    // 1 Course có nhiều CourseImage
    public List<CourseImage> Images { get; set; } = new();

    // Chỉ tính khi hiển thị, KHÔNG tạo cột trong database (Chức năng 3)
    [NotMapped]
    [Display(Name = "Phân loại")]
    public string DurationType => Duration <= 30 ? "Ngắn hạn" : "Dài hạn";

    // Ràng buộc 2: IsActive = false => "Ngừng tuyển sinh"
    [NotMapped]
    [Display(Name = "Trạng thái")]
    public string StatusText => IsActive ? "Đang tuyển sinh" : "Ngừng tuyển sinh";
}
