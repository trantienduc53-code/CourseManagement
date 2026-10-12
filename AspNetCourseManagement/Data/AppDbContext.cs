using CourseManagement.Models;
using Microsoft.EntityFrameworkCore;

namespace CourseManagement.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<CourseCategory> CourseCategories => Set<CourseCategory>();
    public DbSet<Course> Courses => Set<Course>();
    public DbSet<CourseImage> CourseImages => Set<CourseImage>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<CourseCategory>(e =>
        {
            e.ToTable("CourseCategories");
            e.Property(c => c.Name).HasMaxLength(100).IsRequired();
        });

        modelBuilder.Entity<Course>(e =>
        {
            e.ToTable("Courses");
            e.Property(c => c.Name).HasMaxLength(200).IsRequired();
            e.Property(c => c.TuitionFee).HasColumnType("decimal(18,2)");

            // 1 CourseCategory - n Course. Restrict: không cho xóa loại khi còn khóa học (Ràng buộc 1)
            e.HasOne(c => c.CourseCategory)
                .WithMany(cc => cc.Courses)
                .HasForeignKey(c => c.CourseCategoryId)
                .OnDelete(DeleteBehavior.Restrict);

            // Tên khóa học không trùng trong cùng một loại khóa học
            e.HasIndex(c => new { c.CourseCategoryId, c.Name }).IsUnique();
        });

        modelBuilder.Entity<CourseImage>(e =>
        {
            e.ToTable("CourseImages");
            e.Property(i => i.ImageUrl).HasMaxLength(500).IsRequired();

            // 1 Course - n CourseImage
            e.HasOne(i => i.Course)
                .WithMany(c => c.Images)
                .HasForeignKey(i => i.CourseId)
                .OnDelete(DeleteBehavior.Cascade);

            // Mỗi khóa học tối đa MỘT ảnh đại diện (ràng buộc ở mức database)
            e.HasIndex(i => i.CourseId)
                .IsUnique()
                .HasFilter("[IsThumbnail] = 1");
        });
    }
}
