package com.example.coursemanagement;

import com.example.coursemanagement.model.Course;
import com.example.coursemanagement.model.CourseCategory;
import com.example.coursemanagement.model.CourseImage;
import com.example.coursemanagement.repository.CourseCategoryRepository;
import com.example.coursemanagement.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm tra các yêu cầu chính của đề bài bằng profile "h2" (database trong bộ nhớ).
 * Mỗi test chạy trong transaction và được rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class CourseManagementApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseCategoryRepository categoryRepository;

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String getBody(String url) throws Exception {
        return body(mvc.perform(get(url)).andExpect(status().isOk()).andReturn());
    }

    private Course course(String name) {
        return courseRepository.findAll().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
    }

    private CourseCategory category(String name) {
        return categoryRepository.findAll().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
    }

    // ---------- Dữ liệu mẫu ----------
    @Test
    void seedHasAtLeast5CoursesAnd3Categories() {
        assertTrue(courseRepository.count() >= 5);
        assertTrue(categoryRepository.count() >= 3);
    }

    // ---------- Chức năng 2: tìm kiếm / lọc / sắp xếp ----------
    @Test
    void listShowsCategoryNameAndDurationType() throws Exception {
        String html = getBody("/courses");
        assertTrue(html.contains("Lập trình Web"));
        assertTrue(html.contains("Ngắn hạn"));
        assertTrue(html.contains("Dài hạn"));
        assertTrue(html.contains("Ngừng tuyển sinh"), "khóa học IsActive=false vẫn hiển thị với trạng thái Ngừng tuyển sinh");
    }

    @Test
    void searchByPartOfName() throws Exception {
        String html = getBody("/courses?keyword=react");
        assertTrue(html.contains("ReactJS"));
        assertFalse(html.contains("Flutter"));
        assertTrue(html.contains("value=\"react\""), "điều kiện đã nhập phải còn trên form");
    }

    @Test
    void searchWithNoResultShowsMessage() throws Exception {
        String html = getBody("/courses?keyword=khong-ton-tai-xyz");
        assertTrue(html.contains("Không tìm thấy khóa học nào phù hợp"));
    }

    @Test
    void filterByCategory() throws Exception {
        Integer mobileId = category("Lập trình Di động").getId();
        String html = getBody("/courses?categoryId=" + mobileId);
        assertTrue(html.contains("Flutter"));
        assertFalse(html.contains("ReactJS"));
    }

    @Test
    void filterActiveOnlyHidesInactive() throws Exception {
        String active = getBody("/courses?status=active");
        assertFalse(active.contains("Power BI"));
        assertTrue(active.contains("ReactJS"));

        String inactive = getBody("/courses?status=inactive");
        assertTrue(inactive.contains("Power BI"));
        assertFalse(inactive.contains("ReactJS"));
    }

    @Test
    void filterByFeeRange() throws Exception {
        String html = getBody("/courses?minFee=2500000&maxFee=3000000");
        assertTrue(html.contains("SQL Server"));      // 2.500.000
        assertTrue(html.contains("Power BI"));        // 2.800.000
        assertFalse(html.contains("ReactJS"));        // 3.200.000
        assertFalse(html.contains("ASP.NET Core"));   // 4.500.000
    }

    @Test
    void minFeeGreaterThanMaxFeeShowsErrorWithoutCrash() throws Exception {
        String html = getBody("/courses?minFee=5000000&maxFee=1000000");
        assertTrue(html.contains("Học phí tối thiểu không được lớn hơn học phí tối đa."));
    }

    @Test
    void invalidFeeTextDoesNotCrash() throws Exception {
        String html = getBody("/courses?minFee=abc");
        assertTrue(html.contains("không hợp lệ"));
    }

    @Test
    void sortByFeeAndDuration() throws Exception {
        String desc = getBody("/courses?sort=fee_desc");
        assertTrue(desc.indexOf("ASP.NET Core") < desc.indexOf("ReactJS"));   // 4.5tr trước 3.2tr

        String asc = getBody("/courses?sort=fee_asc");
        assertTrue(asc.indexOf("HTML/CSS") < asc.indexOf("ASP.NET Core"));    // 1.8tr trước 4.5tr

        String duration = getBody("/courses?sort=duration_asc");
        assertTrue(duration.indexOf("HTML/CSS") < duration.indexOf("ASP.NET Core")); // 20h trước 40h
    }

    @Test
    void combinedFilters() throws Exception {
        Integer webId = category("Lập trình Web").getId();
        String html = getBody("/courses?keyword=a&categoryId=" + webId + "&status=active&minFee=1000000&maxFee=4000000&sort=fee_desc");
        assertTrue(html.contains("ReactJS"));
        assertFalse(html.contains("ASP.NET Core"));   // 4.5tr vượt max
        assertFalse(html.contains("Flutter"));        // khác loại
    }

    // ---------- Chức năng 3: phân loại thời lượng ----------
    @Test
    void durationTypeBoundary() {
        Course c = new Course();
        c.setDuration(30);
        assertEquals("Ngắn hạn", c.getDurationType());
        c.setDuration(31);
        assertEquals("Dài hạn", c.getDurationType());
    }

    // ---------- Chức năng 4: ảnh ----------
    @Test
    void addImageWithThumbnailKeepsOnlyOneThumbnail() throws Exception {
        Integer id = course("ASP.NET Core").getId();

        mvc.perform(post("/courses/" + id + "/images")
                        .param("imageUrl", "https://example.com/new.png").param("thumbnail", "true"))
                .andExpect(status().is3xxRedirection());

        Course reloaded = courseRepository.findWithDetailsById(id).orElseThrow();
        long thumbnails = reloaded.getImages().stream().filter(CourseImage::isThumbnail).count();
        assertEquals(1, thumbnails);
        assertEquals("https://example.com/new.png", reloaded.getThumbnailImage().getImageUrl());
        assertEquals(4, reloaded.getImages().size());
    }

    @Test
    void changeThumbnailTurnsOldOneIntoNormalImage() throws Exception {
        Course aspnet = courseRepository.findWithDetailsById(course("ASP.NET Core").getId()).orElseThrow();
        CourseImage old = aspnet.getThumbnailImage();
        CourseImage other = aspnet.getImages().stream().filter(i -> !i.isThumbnail()).findFirst().orElseThrow();
        Integer oldId = old.getId();
        Integer otherId = other.getId();

        mvc.perform(post("/courses/images/" + otherId + "/thumbnail")).andExpect(status().is3xxRedirection());

        Course reloaded = courseRepository.findWithDetailsById(aspnet.getId()).orElseThrow();
        assertEquals(1, reloaded.getImages().stream().filter(CourseImage::isThumbnail).count());
        assertEquals(otherId, reloaded.getThumbnailImage().getId());
        assertFalse(reloaded.getImages().stream().filter(i -> i.getId().equals(oldId)).findFirst().orElseThrow().isThumbnail());
    }

    @Test
    void invalidImageUrlIsRejectedWithMessage() throws Exception {
        Integer id = course("ReactJS").getId();
        int before = courseRepository.findWithDetailsById(id).orElseThrow().getImages().size();

        mvc.perform(post("/courses/" + id + "/images").param("imageUrl", "not a url"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertEquals(before, courseRepository.findWithDetailsById(id).orElseThrow().getImages().size());
    }

    @Test
    void courseWithoutImagesShowsMessageOnDetail() throws Exception {
        Integer id = course("Power BI phân tích dữ liệu").getId();
        assertTrue(getBody("/courses/" + id).contains("chưa có ảnh"));
    }

    @Test
    void detailShowsAllImages() throws Exception {
        Integer id = course("ASP.NET Core").getId();
        String html = getBody("/courses/" + id);
        assertTrue(html.contains("aspnet-1"));
        assertTrue(html.contains("aspnet-2"));
        assertTrue(html.contains("aspnet-3"));
    }

    // ---------- Chức năng 5: ràng buộc ----------
    @Test
    void cannotDeleteCategoryThatStillHasCourses() throws Exception {
        Integer webId = category("Lập trình Web").getId();

        mvc.perform(post("/categories/" + webId + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertTrue(categoryRepository.existsById(webId));
    }

    @Test
    void canDeleteEmptyCategory() throws Exception {
        CourseCategory empty = categoryRepository.save(new CourseCategory("Trống", "không có khóa học"));

        mvc.perform(post("/categories/" + empty.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        assertFalse(categoryRepository.existsById(empty.getId()));
    }

    // ---------- Validation Create / Edit ----------
    @Test
    void createWithInvalidDataShowsValidationMessages() throws Exception {
        String html = body(mvc.perform(post("/courses/create")
                        .param("name", "   ").param("tuitionFee", "0").param("duration", "0").param("courseCategoryId", ""))
                .andExpect(status().isOk()).andReturn());
        assertTrue(html.contains("Tên khóa học không được để trống."));
        assertTrue(html.contains("Học phí phải lớn hơn 0."));
        assertTrue(html.contains("Thời lượng phải lớn hơn 0."));
        assertTrue(html.contains("Vui lòng chọn loại khóa học."));
    }

    @Test
    void createWithNonNumericFeeShowsFriendlyMessage() throws Exception {
        String html = body(mvc.perform(post("/courses/create")
                        .param("name", "Abc").param("tuitionFee", "abc").param("duration", "10")
                        .param("courseCategoryId", category("Lập trình Web").getId().toString()))
                .andExpect(status().isOk()).andReturn());
        assertTrue(html.contains("Học phí phải là một số hợp lệ."));
        assertFalse(html.contains("Failed to convert"));
    }

    @Test
    void createWithUnknownCategoryIsRejected() throws Exception {
        String html = body(mvc.perform(post("/courses/create")
                        .param("name", "Khóa mới").param("tuitionFee", "1000").param("duration", "10")
                        .param("courseCategoryId", "99999"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(html.contains("Loại khóa học không tồn tại."));
    }

    @Test
    void duplicateNameInSameCategoryIsRejectedButAllowedInAnotherCategory() throws Exception {
        Integer webId = category("Lập trình Web").getId();
        Integer dataId = category("Cơ sở dữ liệu").getId();

        String html = body(mvc.perform(post("/courses/create")
                        .param("name", "reactjs").param("tuitionFee", "1000").param("duration", "10")
                        .param("courseCategoryId", webId.toString()))
                .andExpect(status().isOk()).andReturn());
        assertTrue(html.contains("Tên khóa học đã tồn tại trong loại khóa học này."));

        long before = courseRepository.count();
        mvc.perform(post("/courses/create")
                        .param("name", "ReactJS").param("tuitionFee", "1000").param("duration", "10")
                        .param("courseCategoryId", dataId.toString()))
                .andExpect(status().is3xxRedirection());
        assertEquals(before + 1, courseRepository.count());
    }

    @Test
    void editChangesCourse() throws Exception {
        Course react = course("ReactJS");
        mvc.perform(post("/courses/" + react.getId() + "/edit")
                        .param("name", "ReactJS nâng cao").param("tuitionFee", "3500000").param("duration", "45")
                        .param("courseCategoryId", react.getCategory().getId().toString())
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection());

        Course updated = courseRepository.findById(react.getId()).orElseThrow();
        assertEquals("ReactJS nâng cao", updated.getName());
        assertEquals("Dài hạn", updated.getDurationType());
    }

    @Test
    void editWithInvalidDataShowsForm() throws Exception {
        Integer id = course("ReactJS").getId();
        String html = body(mvc.perform(get("/courses/" + id + "/edit")).andExpect(status().isOk()).andReturn());
        assertTrue(html.contains("ReactJS"));

        String invalid = body(mvc.perform(post("/courses/" + id + "/edit")
                        .param("name", "").param("tuitionFee", "-5").param("duration", "10").param("courseCategoryId", "1"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(invalid.contains("Tên khóa học không được để trống."));
        assertTrue(invalid.contains("Học phí phải lớn hơn 0."));
    }

    // ---------- Xử lý lỗi: không để lỗi hệ thống ra màn hình ----------
    @Test
    void unknownCourseIdShowsFriendly404() throws Exception {
        for (String url : new String[]{"/courses/99999", "/courses/99999/edit", "/courses/abc"}) {
            String html = body(mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn());
            assertTrue(html.contains("Không tìm thấy"), url);
            assertFalse(html.contains("Exception"), url);
        }
    }

    @Test
    void unknownCategoryAndImageShowFriendly404() throws Exception {
        mvc.perform(get("/categories/99999/edit")).andExpect(status().isNotFound());
        mvc.perform(post("/courses/images/99999/thumbnail")).andExpect(status().isNotFound());
        mvc.perform(post("/courses/images/99999/delete")).andExpect(status().isNotFound());
        mvc.perform(post("/courses/99999/delete")).andExpect(status().isNotFound());
    }

    @Test
    void unknownUrlShowsFriendly404() throws Exception {
        mvc.perform(get("/khong-co-trang-nay")).andExpect(status().isNotFound());
    }
}
