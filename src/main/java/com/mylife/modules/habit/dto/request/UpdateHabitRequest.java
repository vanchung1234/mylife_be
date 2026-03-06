package com.mylife.modules.habit.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO request dùng để cập nhật thói quen.
 *
 * Tất cả field đều optional:
 * - Nếu field = null → giữ nguyên giá trị cũ
 * - Nếu field != null → cập nhật sang giá trị mới
 *
 * Việc validate nghiệp vụ chi tiết (ví dụ: không cho đổi type sau khi tạo) sẽ được xử lý ở {@link com.mylife.modules.habit.service.HabitService}.
 */
@Data
public class UpdateHabitRequest {

    /** Tên mới cho thói quen (nếu muốn đổi). */
    private String name;

    /** Mô tả mới (nếu muốn đổi). */
    private String description;

    /** Icon mới cho thói quen. */
    private String icon;

    /** Màu mới cho thói quen (mã hex). */
    private String color;

    /**
     * Mục tiêu mới (goal).
     * - NUMERIC: số lượng mới
     * - DURATION: số phút mới
     */
    private Double goal;

    /** Đơn vị mới cho goal. */
    private String unit;

    /**
     * Cấu hình frequencyData mới (JSON string).
     * - Khi cập nhật, service sẽ validate dựa trên frequencyType hiện tại của habit.
     */
    private String frequencyData;

    /** Ngày kết thúc mới (có thể rút ngắn hoặc kéo dài). */
    private LocalDate endDate;

    /** Trạng thái tạm dừng (override togglePause). */
    private Boolean paused;

    /** Giờ nhắc nhở mới. */
    private LocalTime reminderTime;

    /** Bật/tắt nhắc cuối ngày. */
    private Boolean remindEndOfDay;
}