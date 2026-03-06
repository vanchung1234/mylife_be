package com.mylife.modules.habit.entity;

import com.mylife.common.base.BaseEntity;
import com.mylife.modules.habit.entity.enums.FrequencyType;
import com.mylife.modules.habit.entity.enums.HabitType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "habits")
public class Habit extends BaseEntity {

    /**
     * Tên thói quen.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Mô tả chi tiết (tùy chọn).
     */
    private String description;

    /**
     * Icon hiển thị (emoji hoặc tên icon).
     */
    private String icon;

    /**
     * Màu hiển thị (mã hex, ví dụ: #FF5733).
     */
    private String color;

    /**
     * Loại thói quen (BINARY/NUMERIC/DURATION).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitType type;

    /**
     * Mục tiêu (tùy theo loại):
     * - NUMERIC: số lượng cần đạt
     * - DURATION: số phút
     */
    private Double goal;

    /**
     * Đơn vị cho goal (lít, lần, km, phút...).
     */
    private String unit;

    /**
     * Kiểu tần suất lặp lại.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequencyType frequencyType;

    /**
     * Dữ liệu tần suất dạng JSON, ví dụ:
     * - WEEKLY_DAYS: {"daysOfWeek": [1,3,5]}  // 1=Monday, ..., 7=Sunday
     * - MONTHLY_DATES: {"daysOfMonth": [1,15,30]}
     * - CUSTOM_INTERVAL: {"intervalDays": 2}
     *
     * Được parse/validate ở {@link com.mylife.modules.habit.service.HabitService}.
     */
    @Column(columnDefinition = "jsonb")
    private String frequencyData;

    /**
     * Ngày bắt đầu áp dụng habit.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Ngày kết thúc (có thể null = vô thời hạn).
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Trạng thái tạm dừng hay không.
     */
    private Boolean paused = false;

    /**
     * Giờ nhắc nhở trong ngày (có thể null).
     */
    private LocalTime reminderTime;

    /**
     * Có gửi nhắc cuối ngày nếu chưa check-in hay không.
     */
    @Column(name = "remind_end_of_day")
    private Boolean remindEndOfDay = false;

    /**
     * ID của user sở hữu habit này.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Danh sách logs (check-in) của habit.
     * Cascade ALL + orphanRemoval:
     * - Xóa habit → xóa luôn tất cả logs liên quan.
     */
    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HabitLog> logs = new HashSet<>();

    /**
     * Thông tin streak của habit (1-1).
     * Cascade ALL + orphanRemoval:
     * - Xóa habit → xóa luôn streak.
     */
    @OneToOne(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private HabitStreak streak;
}