using System.Globalization;
using CourseManagement.Data;
using Microsoft.AspNetCore.Localization;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews(options =>
{
    // Thông báo lỗi binding bằng tiếng Việt (ví dụ nhập chữ vào ô số, để trống ô số...)
    var messages = options.ModelBindingMessageProvider;
    messages.SetValueMustNotBeNullAccessor(_ => "Vui lòng nhập giá trị.");
    messages.SetMissingBindRequiredValueAccessor(name => $"Thiếu giá trị cho '{name}'.");
    messages.SetValueIsInvalidAccessor(value => $"Giá trị '{value}' không hợp lệ.");
    messages.SetAttemptedValueIsInvalidAccessor((value, field) => $"Giá trị '{value}' không hợp lệ cho {field}.");
    messages.SetValueMustBeANumberAccessor(field => $"{field} phải là một số.");
    messages.SetUnknownValueIsInvalidAccessor(field => $"Giá trị không hợp lệ cho {field}.");
});

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

var app = builder.Build();

// Luôn hiển thị trang lỗi thân thiện, không để lộ lỗi hệ thống ra màn hình
app.UseExceptionHandler("/Home/Error");
app.UseStatusCodePagesWithReExecute("/Home/HttpStatus", "?code={0}");

// Dùng culture en-US để dấu thập phân "." khi nhập học phí được hiểu đúng
var culture = new CultureInfo("en-US");
app.UseRequestLocalization(new RequestLocalizationOptions
{
    DefaultRequestCulture = new RequestCulture(culture),
    SupportedCultures = new[] { culture },
    SupportedUICultures = new[] { culture }
});

app.UseStaticFiles();
app.UseRouting();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Courses}/{action=Index}/{id?}");

// Tạo database + 3 bảng và nạp dữ liệu mẫu khi chạy lần đầu
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    if (db.Database.GetMigrations().Any())
    {
        db.Database.Migrate();          // nếu bạn đã tạo migration bằng dotnet ef
    }
    else
    {
        db.Database.EnsureCreated();    // mặc định: tạo bảng trực tiếp từ model
    }
    DbSeeder.Seed(db);
}

app.Run();
