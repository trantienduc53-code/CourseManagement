using Microsoft.AspNetCore.Mvc;

namespace CourseManagement.Controllers;

public class HomeController : Controller
{
    public IActionResult Index() => RedirectToAction("Index", "Courses");

    // Được gọi bởi UseExceptionHandler khi có lỗi không lường trước
    public IActionResult Error()
    {
        Response.StatusCode = StatusCodes.Status500InternalServerError;
        return View("Error");
    }

    // Được gọi bởi UseStatusCodePagesWithReExecute (404, 405...)
    public IActionResult HttpStatus(int code)
    {
        if (code == StatusCodes.Status404NotFound)
        {
            return View("NotFoundPage", "Trang bạn truy cập không tồn tại.");
        }
        return View("Error");
    }
}
