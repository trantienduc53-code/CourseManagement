using System.ComponentModel.DataAnnotations;

namespace CourseManagement.Models;

public class CourseImage
{
    public int Id { get; set; }

    [Required(ErrorMessage = "Đường dẫn ảnh không được để trống.")]
    [StringLength(500, ErrorMessage = "Đường dẫn ảnh tối đa 500 ký tự.")]
    [Display(Name = "Đường dẫn ảnh")]
    public string ImageUrl { get; set; } = string.Empty;

    [Display(Name = "Ảnh đại diện")]
    public bool IsThumbnail { get; set; }

    public int CourseId { get; set; }

    public Course? Course { get; set; }
}
