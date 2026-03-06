package com.mylife.common.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp cơ sở cho tất cả các Entity.
 * Cung cấp các trường chung:
 * - id: UUID (tự động sinh)
 * - createdAt: thời gian tạo
 * - updatedAt: thời gian cập nhật gần nhất
 * 
 * Sử dụng @MappedSuperclass để JPA hiểu rằng lớp này không phải entity riêng
 * mà chỉ cung cấp ánh xạ cho các lớp con.
 */
@Getter
@Setter
@MappedSuperclass // Đánh dấu đây là lớp cơ sở, không tạo bảng riêng
@EntityListeners(AuditingEntityListener.class) // Lắng nghe sự kiện auditing của JPA
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Tự động sinh UUID
    @Column(columnDefinition = "UUID") // Chỉ rõ kiểu dữ liệu cột là UUID (PostgreSQL)
    private UUID id;

    @CreationTimestamp // Hibernate tự động set thời gian khi insert
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Hibernate tự động set thời gian khi update
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Phương thức này được gọi trước khi entity được persist (insert) lần đầu.
     * Có thể dùng để set các giá trị mặc định.
     */
    @PrePersist
    protected void onCreate() {
        // Nếu cần set thêm giá trị gì trước khi lưu
    }

    /**
     * Phương thức này được gọi trước khi entity được update.
     */
    @PreUpdate
    protected void onUpdate() {
        // Nếu cần xử lý trước khi update
    }
}