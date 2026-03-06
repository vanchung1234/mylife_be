package com.mylife.modules.habit.dto.request;

import com.mylife.modules.habit.entity.enums.FrequencyType;
import com.mylife.modules.habit.entity.enums.HabitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO request dùng để tạo thói quen mới.
 *
 * Các field chính:
 * - name: Tên thói quen (bắt buộc)
 * - type: Loại thói quen (BINARY/NUMERIC/DURATION) (bắt buộc)
 * - goal: Mục tiêu (bắt buộc nếu type = NUMERIC hoặc DURATION, ví dụ 2 lít nước, 30 phút)
 * - unit: Đơn vị (lần, lít, phút...)
 * - frequencyType: Kiểu tần suất (DAILY, WEEKLY_DAYS, MONTHLY_DATES, CUSTOM_INTERVAL) (bắt buộc)
 * - frequencyData: JSON mô tả chi tiết tần suất (tùy theo frequencyType)
 * - startDate / endDate: Ngày bắt đầu/kết thúc thói quen
 * - reminderTime: Giờ nhắc nhở trong ngày (nếu có)
 * - remindEndOfDay: Có gửi nhắc cuối ngày nếu chưa check-in không
 */
@Data
public class CreateHabitRequest {

    /**
     * Tên thói quen hiển thị cho user.
     * Ví dụ: "Uống nước", "Tập thể dục", "Đọc sách".
     */
    @NotBlank
    private String name;

    /**
     * Mô tả chi tiết hơn về thói quen (tùy chọn).
     */
    private String description;

    /**
     * Icon đại diện cho thói quen (emoji hoặc tên icon).
     * Ví dụ: "💧", "🏃‍♂️".
     */
    private String icon;

    /**
     * Màu hiển thị (mã hex), ví dụ "#FF5733".
     */
    private String color;

    /**
     * Loại thói quen:
     * - BINARY: chỉ cần biết đã làm hay chưa (true/false)
     * - NUMERIC: theo số lượng (số lần, lít...)
     * - DURATION: theo thời lượng (phút, giờ...)
     */
    @NotNull
    private HabitType type;

    /**
     * Mục tiêu cần đạt:
     * - NUMERIC: số lượng (ví dụ: 2 lít nước)
     * - DURATION: số phút (ví dụ: 30 phút đọc sách)
     * Nếu type = BINARY thì có thể để null.
     */
    private Double goal;

    /**
     * Đơn vị cho goal (ví dụ: "lít", "lần", "phút").
     */
    private String unit;

    /**
     * Kiểu tần suất:
     * - DAILY: mỗi ngày
     * - WEEKLY_DAYS: các ngày trong tuần (ví dụ thứ 2,4,6)
     * - MONTHLY_DATES: các ngày trong tháng (ngày 1,15,30)
     * - CUSTOM_INTERVAL: mỗi X ngày một lần
     */
    @NotNull
    private FrequencyType frequencyType;

    /**
     * Cấu hình chi tiết tần suất dạng JSON, phụ thuộc vào frequencyType:
     *
     * DAILY:
     * - Có thể null hoặc empty.
     *
     * WEEKLY_DAYS:
     * - {"daysOfWeek": [1,3,5]}  // 1=Monday, ..., 7=Sunday
     *
     * MONTHLY_DATES:
     * - {"daysOfMonth": [1,15,30]}
     *
     * CUSTOM_INTERVAL:
     * - {"intervalDays": 2}  // Mỗi 2 ngày một lần
     */
    private String frequencyData;

    /**
     * Ngày bắt đầu áp dụng thói quen.
     */
    @NotNull
    private LocalDate startDate;

    /**
     * Ngày kết thúc (có thể null = vô thời hạn).
     */
    private LocalDate endDate;

    /**
     * Giờ trong ngày để gửi nhắc nhở (có thể null).
     */
    private LocalTime reminderTime;

    /**
     * Có gửi thêm nhắc cuối ngày nếu chưa check-in không.
     */
    private Boolean remindEndOfDay = false;
}