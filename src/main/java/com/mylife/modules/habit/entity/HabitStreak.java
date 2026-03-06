package com.mylife.modules.habit.entity;

import com.mylife.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entity lưu thông tin streak cho một Habit.
 *
 * Streak được tính dựa trên các HabitLog completed = true:
 * - currentStreak: số ngày liên tiếp gần nhất
 * - longestStreak: chuỗi ngày liên tiếp dài nhất
 * - lastLogDate: ngày gần nhất có log completed = true
 *
 * Việc tính toán/ cập nhật được thực hiện trong {@link com.mylife.modules.habit.service.HabitService}.
 */
@Getter
@Setter
@Entity
@Table(name = "habit_streaks")
public class HabitStreak extends BaseEntity {

    /**
     * Habit tương ứng (1-1).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", unique = true, nullable = false)
    private Habit habit;

    /**
     * Chuỗi ngày liên tiếp hiện tại.
     */
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    /**
     * Chuỗi ngày liên tiếp dài nhất đã đạt được.
     */
    @Column(name = "longest_streak", nullable = false)
    private Integer longestStreak = 0;

    /**
     * Ngày gần nhất có log completed = true.
     */
    @Column(name = "last_log_date")
    private LocalDate lastLogDate;
}