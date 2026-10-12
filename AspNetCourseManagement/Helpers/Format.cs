using System.Globalization;

namespace CourseManagement.Helpers;

public static class Format
{
    private static readonly CultureInfo Vn = new("vi-VN");

    public static string Vnd(decimal value) => string.Format(Vn, "{0:N0} ₫", value);
}
