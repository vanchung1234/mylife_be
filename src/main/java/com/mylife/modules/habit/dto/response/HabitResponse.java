package com.mylife.modules.habit.dto.response;

import com.mylife.modules.habit.entity.enums.FrequencyType;
import com.mylife.modules.habit.entity.enums.HabitType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO trả về cho thông tin chi tiết một habit.
 *
 * Được dùng cho:
 * - API tạo habit (response sau khi tạo)
 * - API lấy danh sách tất cả habits
 * - API lấy chi tiết một habit
 *
 * Bao gồm cả thông tin cấu hình lẫn trạng thái streak hiện tại.
 */
@Data
@Builder
public class HabitResponse {

    /** ID của habit (UUID). */
    private UUID id;

    /** Tên thói quen. */
    private String name;

    /** Mô tả chi tiết. */
    private String description;

    /** Icon hiển thị (emoji / tên icon). */
    private String icon;

    /** Màu hiển thị (mã hex). */
    private String color;

    /** Loại thói quen (BINARY, NUMERIC, DURATION). */
    private HabitType type;

    /** Mục tiêu (goal) nếu áp dụng. */
    private Double goal;

    /** Đơn vị cho goal. */
    private String unit;

    /** Kiểu tần suất. */
    private FrequencyType frequencyType;

    /** JSON mô tả chi tiết tần suất (frequencyData). */
    private String frequencyData;

    /** Ngày bắt đầu áp dụng. */
    private LocalDate startDate;

    /** Ngày kết thúc (nếu có). */
    private LocalDate endDate;

    /** Trạng thái tạm dừng hay không. */
    private Boolean paused;

    /** Giờ nhắc nhở trong ngày. */
    private LocalTime reminderTime;

    /** Có nhắc cuối ngày nếu chưa check-in hay không. */
    private Boolean remindEndOfDay;

    /** Chuỗi ngày liên tiếp hiện tại. */
    private Integer currentStreak;

    /** Chuỗi ngày liên tiếp dài nhất từ trước đến nay. */
    private Integer longestStreak;
}