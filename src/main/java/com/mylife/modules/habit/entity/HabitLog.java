package com.mylife.modules.habit.entity;

import com.mylife.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entity đại diện cho một lần check-in (log) của Habit.
 *
 * Mỗi HabitLog gắn với:
 * - Một Habit cụ thể
 * - Một ngày cụ thể (logDate)
 *
 * Dùng để:
 * - Theo dõi lịch sử hoàn thành thói quen
 * - Tính streak, thống kê tỷ lệ hoàn thành
 */
@Getter
@Setter
@Entity
@Table(name = "habit_logs")
public class HabitLog extends BaseEntity {

    /**
     * Habit mà log này thuộc về.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    /**
     * Ngày check-in (theo múi giờ của user).
     * Thông thường một habit chỉ có tối đa 1 log cho mỗi ngày.
     */
    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    /**
     * Trạng thái hoàn thành:
     * - BINARY: true/false
     * - NUMERIC/DURATION: được suy ra từ value và goal
     */
    private Boolean completed;

    /**
     * Giá trị đạt được (cho habit NUMERIC/DURATION).
     */
    private Double value;

    /**
     * Ghi chú đi kèm log.
     */
    private String note;

    /**
     * Có phải là check-in bù (cho một ngày trong quá khứ) hay không.
     */
    @Column(name = "is_makeup")
    private Boolean isMakeup = false;
}