package com.example.coursemanagement.repository;

import com.example.coursemanagement.model.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseImageRepository extends JpaRepository<CourseImage, Integer> {

    boolean existsByCourseIdAndThumbnailTrue(Integer courseId);

    /** Bỏ cờ ảnh đại diện của các ảnh khác trong cùng khóa học (ảnh đại diện cũ -> ảnh thường). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CourseImage i set i.thumbnail = false "
            + "where i.course.id = :courseId and i.thumbnail = true and i.id <> :imageId")
    int clearOtherThumbnails(@Param("courseId") Integer courseId, @Param("imageId") Integer imageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CourseImage i set i.thumbnail = true where i.id = :imageId")
    int markAsThumbnail(@Param("imageId") Integer imageId);
}
