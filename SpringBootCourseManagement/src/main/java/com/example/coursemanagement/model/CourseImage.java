package com.example.coursemanagement.model;

import jakarta.persistence.*;

@Entity
@Table(name = "CourseImages")
public class CourseImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "ImageUrl", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "IsThumbnail", nullable = false)
    private boolean thumbnail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseId", nullable = false)
    private Course course;

    public CourseImage() {
    }

    public CourseImage(Course course, String imageUrl, boolean thumbnail) {
        this.course = course;
        this.imageUrl = imageUrl;
        this.thumbnail = thumbnail;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isThumbnail() { return thumbnail; }
    public void setThumbnail(boolean thumbnail) { this.thumbnail = thumbnail; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
}
