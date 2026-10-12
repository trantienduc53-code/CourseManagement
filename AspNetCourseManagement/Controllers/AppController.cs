using Microsoft.AspNetCore.Mvc;

namespace CourseManagement.Controllers;

public abstract class AppController : Controller
{
    /// <summary>Trả về trang 404 thân thiện (không để lộ lỗi hệ thống).</summary>
    protected IActionResult NotFoundPage(string message)
    {
        Response.StatusCode = StatusCodes.Status404NotFound;
        return View("NotFoundPage", message);
    }
}
