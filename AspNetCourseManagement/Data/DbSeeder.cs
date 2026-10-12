using CourseManagement.Models;

namespace CourseManagement.Data;

public static class DbSeeder
{
    public static void Seed(AppDbContext db)
    {
        if (db.CourseCategories.Any()) return;

        var web = new CourseCategory { Name = "Lập trình Web", Description = "Các khóa học xây dựng website và ứng dụng web." };
        var mobile = new CourseCategory { Name = "Lập trình Di động", Description = "Các khóa học phát triển ứng dụng di động." };
        var data = new CourseCategory { Name = "Cơ sở dữ liệu", Description = "Các khóa học về cơ sở dữ liệu và phân tích dữ liệu." };
        db.CourseCategories.AddRange(web, mobile, data);

        var aspnet = new Course { Name = "ASP.NET Core", TuitionFee = 4_500_000, Duration = 40, IsActive = true, Description = "Xây dựng ứng dụng web với ASP.NET Core MVC và Entity Framework Core.", CourseCategory = web };
        var react = new Course { Name = "ReactJS", TuitionFee = 3_200_000, Duration = 24, IsActive = true, Description = "Xây dựng giao diện web hiện đại với ReactJS.", CourseCategory = web };
        var html = new Course { Name = "HTML/CSS/JavaScript nhập môn", TuitionFee = 1_800_000, Duration = 20, IsActive = true, Description = "Nền tảng frontend cho người mới bắt đầu.", CourseCategory = web };
        var flutter = new Course { Name = "Flutter cho người mới", TuitionFee = 3_900_000, Duration = 36, IsActive = true, Description = "Lập trình ứng dụng đa nền tảng với Flutter.", CourseCategory = mobile };
        var sql = new Course { Name = "SQL Server căn bản", TuitionFee = 2_500_000, Duration = 30, IsActive = true, Description = "Thiết kế và truy vấn cơ sở dữ liệu với SQL Server.", CourseCategory = data };
        var powerBi = new Course { Name = "Power BI phân tích dữ liệu", TuitionFee = 2_800_000, Duration = 28, IsActive = false, Description = "Trực quan hóa dữ liệu với Power BI (tạm ngừng tuyển sinh).", CourseCategory = data };
        db.Courses.AddRange(aspnet, react, html, flutter, sql, powerBi);

        // powerBi cố ý không có ảnh để demo trường hợp "Khóa học chưa có ảnh"
        db.CourseImages.AddRange(
            new CourseImage { Course = aspnet, ImageUrl = "https://picsum.photos/seed/aspnet-1/600/400", IsThumbnail = true },
            new CourseImage { Course = aspnet, ImageUrl = "https://picsum.photos/seed/aspnet-2/600/400", IsThumbnail = false },
            new CourseImage { Course = aspnet, ImageUrl = "https://picsum.photos/seed/aspnet-3/600/400", IsThumbnail = false },
            new CourseImage { Course = react, ImageUrl = "https://picsum.photos/seed/react-1/600/400", IsThumbnail = true },
            new CourseImage { Course = react, ImageUrl = "https://picsum.photos/seed/react-2/600/400", IsThumbnail = false },
            new CourseImage { Course = html, ImageUrl = "https://picsum.photos/seed/html-1/600/400", IsThumbnail = true },
            new CourseImage { Course = flutter, ImageUrl = "https://picsum.photos/seed/flutter-1/600/400", IsThumbnail = true },
            new CourseImage { Course = sql, ImageUrl = "https://picsum.photos/seed/sql-1/600/400", IsThumbnail = true },
            new CourseImage { Course = sql, ImageUrl = "https://picsum.photos/seed/sql-2/600/400", IsThumbnail = false }
        );

        db.SaveChanges();
    }
}
