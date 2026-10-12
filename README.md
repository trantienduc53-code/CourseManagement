# Bài ôn luyện giữa kỳ – Quản lý khóa học

Cùng một đề bài, làm thành **2 project độc lập** (cùng thiết kế 3 bảng, cùng giao diện, cùng SQL Server):

| Thư mục | Công nghệ | Chạy bằng |
|---|---|---|
| `AspNetCourseManagement/` | ASP.NET Core MVC 8 + Entity Framework Core (đúng đề gốc) | `dotnet run` |
| `SpringBootCourseManagement/` | Java Spring Boot 3 + Thymeleaf + JPA/Hibernate | `mvn spring-boot:run` |

> **Lưu ý:** ASP.NET (C#) và Spring Boot (Java) là hai nền tảng khác nhau, không chạy chung được. Nếu giảng viên yêu cầu ASP.NET thì nộp project thứ nhất; nếu cho phép Java thì nộp project thứ hai.
>
> Cả hai project được viết và rà soát kỹ, nhưng **chưa được build/chạy thử** trong môi trường tạo ra chúng (không có .NET SDK, và mạng bị chặn nên không tải được thư viện Maven). Hãy chạy thử trên máy bạn trước buổi thi, và báo lại nếu có lỗi biên dịch.

---

## 1. Chuẩn bị SQL Server

Cần một SQL Server chạy trên máy (SQL Server Express/Developer, LocalDB, hoặc Docker).

Docker (nhanh nhất):

```bash
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=YourStrong@Passw0rd" -p 1433:1433 -d mcr.microsoft.com/mssql/server:2022-latest
```

### ASP.NET
Sửa chuỗi kết nối trong `AspNetCourseManagement/appsettings.json`:

- Windows Authentication (mặc định): `Server=localhost;Database=CourseManagementDb;Trusted_Connection=True;TrustServerCertificate=True`
- Tài khoản sa: `Server=localhost,1433;Database=CourseManagementDb;User Id=sa;Password=YourStrong@Passw0rd;TrustServerCertificate=True`
- LocalDB: `Server=(localdb)\\MSSQLLocalDB;Database=CourseManagementDb;Trusted_Connection=True`

Ứng dụng **tự tạo database + 3 bảng + dữ liệu mẫu** khi chạy lần đầu.

### Spring Boot
Tạo sẵn database rỗng (JDBC không tự tạo database):

```sql
CREATE DATABASE CourseManagementDb;
```

Rồi sửa `spring.datasource.*` trong `SpringBootCourseManagement/src/main/resources/application.properties` (hoặc đặt biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). Hibernate tự tạo 3 bảng và dữ liệu mẫu.

---

## 2. Chạy

```bash
# ASP.NET  ->  http://localhost:5080
cd AspNetCourseManagement
dotnet run

# Spring Boot  ->  http://localhost:8080
cd SpringBootCourseManagement
mvn spring-boot:run
```

Muốn thử Spring Boot nhanh mà **không cần SQL Server**: `mvn spring-boot:run -Dspring-boot.run.profiles=h2` (database trong bộ nhớ, mất dữ liệu khi tắt).
Bộ kiểm thử tự động của Spring Boot (chạy bằng H2): `mvn test`.

Muốn dùng **migration** thay vì tạo bảng trực tiếp (ASP.NET): `dotnet ef migrations add Init` rồi chạy lại; `Program.cs` tự nhận ra có migration và gọi `Migrate()`.

---

## 3. Đối chiếu với đề

| Yêu cầu | ASP.NET | Spring Boot |
|---|---|---|
| 3 model, quan hệ 1–n (Category→Course→Image) | `Models/`, `Data/AppDbContext.cs` | `model/` |
| Validation (tên, học phí > 0, thời lượng > 0, category tồn tại, tên không trùng trong cùng loại) | DataAnnotations + `ValidateBusinessRulesAsync` trong `CoursesController` + unique index | Bean Validation trong `CourseForm` + `checkBusinessRules` trong `CourseController` + unique constraint |
| Create/Edit chọn loại khóa học từ database | `LoadCategoriesAsync` + `Views/Courses/_Form.cshtml` | `categoryService.findAll()` + `courses/form.html` |
| Tìm kiếm + lọc loại + lọc trạng thái + khoảng học phí + sắp xếp, giữ lại điều kiện trên form, **không lọc bằng vòng lặp** | `IQueryable` trong `CoursesController.Index` | JPA `Specification` trong `CourseService.search` |
| Ngắn hạn (≤ 30) / Dài hạn (> 30), không có cột trong DB | `Course.DurationType` (`[NotMapped]`) | `Course.getDurationType()` (`@Transient`) |
| Thêm ảnh bằng URL, xem tất cả ảnh ở Detail, đổi ảnh đại diện (ảnh cũ tự thành ảnh thường, tối đa 1 ảnh đại diện) | `AddImage`, `SetThumbnail`, `DeleteImage` + unique index lọc `IsThumbnail = 1` | `addImage`, `setThumbnail`, `deleteImage` trong `CourseService` |
| Không xóa loại khóa học còn khóa học | `CourseCategoriesController.Delete` (+ FK `Restrict`) | `CategoryService.delete` |
| `IsActive = false` vẫn hiện trong trang quản trị với nhãn "Ngừng tuyển sinh"; bộ lọc "Đang tuyển sinh" chỉ hiện `IsActive = true` | Có | Có |
| Xử lý lỗi: CourseId / CourseCategoryId / CourseImageId không tồn tại, min > max, chưa có ảnh, không có kết quả, Create/Edit không hợp lệ – không lộ lỗi hệ thống | `NotFoundPage`, `UseExceptionHandler`, `UseStatusCodePagesWithReExecute` | `GlobalExceptionHandler`, `templates/error*` |

Dữ liệu mẫu: 3 loại khóa học, 6 khóa học (có khóa học 30 giờ để thấy ranh giới "Ngắn hạn", 1 khóa học đang ngừng tuyển sinh và **chưa có ảnh**, 1 loại khóa học có nhiều ảnh).

---

## 4. Kịch bản demo (14 mục của đề)

1. **Database và 3 bảng**: mở SQL Server Management Studio / Azure Data Studio → database `CourseManagementDb` → bảng `Courses`, `CourseCategories`, `CourseImages`.
2. **Quan hệ**: Database Diagram, hoặc chỉ khóa ngoại `Courses.CourseCategoryId` và `CourseImages.CourseId`.
3. **Danh sách + tên loại**: trang chủ `/Courses` (ASP.NET) hoặc `/courses` (Spring).
4. **Tìm theo tên**: gõ `react`.
5. **Lọc loại**: chọn "Lập trình Web".
6. **Lọc trạng thái**: chọn "Đang tuyển sinh" (Power BI biến mất) rồi "Ngừng tuyển sinh".
7. **Lọc học phí**: min `2500000`, max `3000000`; thử min > max để thấy thông báo lỗi.
8. **Sắp xếp**: học phí tăng/giảm, thời lượng tăng.
9. **Phân loại**: cột "Phân loại" – SQL Server căn bản (30 giờ) là Ngắn hạn, ASP.NET Core (40 giờ) là Dài hạn.
10. **Create/Edit**: thử để trống tên, học phí 0, trùng tên trong cùng loại để thấy validation.
11. **Thêm ảnh**: vào Chi tiết → dán một URL ảnh (ví dụ `https://picsum.photos/seed/abc/600/400`).
12. **Đổi ảnh đại diện**: bấm "Đặt làm ảnh đại diện" ở ảnh khác → ảnh cũ mất nhãn.
13. **Xóa loại đang dùng**: vào "Loại khóa học" → xóa "Lập trình Web" → hiện thông báo không cho xóa.
14. **CourseId không tồn tại**: mở `/Courses/Details/9999` (ASP.NET) hoặc `/courses/9999` (Spring) → trang "Không tìm thấy" thân thiện.

---

## 5. Gợi ý để trả lời khi giảng viên hỏi

- **Vì sao không bị "lấy hết rồi lọc bằng vòng lặp"?** Các điều kiện được ghép vào `IQueryable` (EF) / `Specification` (JPA) nên được dịch thành *một* câu `WHERE ... ORDER BY` chạy trong SQL Server.
- **Làm sao đảm bảo chỉ có một ảnh đại diện?** Khi đổi ảnh, code bỏ cờ của ảnh cũ trước rồi mới bật cờ ảnh mới, trong một transaction. Với ASP.NET còn có unique index lọc `[IsThumbnail] = 1` làm lưới an toàn ở mức database.
- **Vì sao "Ngắn hạn/Dài hạn" không có cột?** Đó là thuộc tính tính toán (`[NotMapped]` / `@Transient`), chỉ suy ra từ `Duration` khi hiển thị.
- **Vì sao không xóa được loại đang dùng?** Code kiểm tra trước và báo lỗi thân thiện; ngoài ra khóa ngoại dùng `Restrict`/ràng buộc FK nên database cũng từ chối.
