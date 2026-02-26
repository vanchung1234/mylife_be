package com.mylife.common.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * @MappedSuperclass
 * Nghĩa là:
 * - Class này KHÔNG phải là một bảng trong database.
 * - Nhưng các class khác kế thừa nó sẽ được thừa hưởng các field này.
 *
 * Ví dụ:
 *   public class User extends BaseEntity
 *
 * → Table "user" sẽ có id, createdAt, updatedAt...
 */
@MappedSuperclass
@Getter   // Lombok tự sinh getter cho tất cả field
@Setter   // Lombok tự sinh setter cho tất cả field
public abstract class BaseEntity {

    /*
     * @Id → Đây là khóa chính (Primary Key)
     *
     * @GeneratedValue(strategy = GenerationType.UUID)
     * → Tự động tạo ID dạng UUID
     *
     * Vì sao dùng UUID thay vì Long?
     * - Không đoán được ID
     * - Không lộ số lượng record
     * - Tốt cho microservice / distributed system
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * @CreationTimestamp
     * → Hibernate tự set thời gian khi record được tạo
     *
     * nullable = false
     * → Không được null trong database
     *
     * updatable = false
     * → Không cho phép sửa sau khi tạo
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
     * @UpdateTimestamp
     * → Hibernate tự cập nhật thời gian mỗi khi entity bị update
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /*
     * createdBy
     * → Lưu ID của user tạo record
     *
     * updatable = false
     * → Không cho sửa sau khi tạo
     *
     * Sau này ta sẽ dùng AuditorAware
     * để tự động set field này.
     */
    @Column(updatable = false)
    private UUID createdBy;

    /*
     * updatedBy
     * → Lưu ID của user sửa record gần nhất
     */
    private UUID updatedBy;

    /*
     * deleted
     * → Soft delete flag
     *
     * Thay vì xóa cứng record khỏi database,
     * ta chỉ đánh dấu deleted = true
     *
     * Vì sao?
     * - Có thể khôi phục lại
     * - Giữ lịch sử
     * - Phù hợp hệ thống enterprise
     */
    @Column(nullable = false)
    private Boolean deleted = false;
}