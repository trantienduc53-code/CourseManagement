using System.ComponentModel.DataAnnotations;

namespace CourseManagement.Models;

public class CourseCategory
{
    public int Id { get; set; }

    [Required(ErrorMessage = "Tên loại khóa học không được để trống.")]
    [StringLength(100, ErrorMessage = "Tên loại khóa học tối đa 100 ký tự.")]
    [Display(Name = "Tên loại khóa học")]
    public string Name { get; set; } = string.Empty;

    [StringLength(500, ErrorMessage = "Mô tả tối đa 500 ký tự.")]
    [Display(Name = "Mô tả")]
    public string? Description { get; set; }

    // 1 CourseCategory có nhiều Course
    public List<Course> Courses { get; set; } = new();
}
