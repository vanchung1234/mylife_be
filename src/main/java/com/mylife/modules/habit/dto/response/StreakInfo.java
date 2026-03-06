package com.mylife.modules.habit.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO thống kê streak và tỷ lệ hoàn thành của một habit.
 *
 * currentStreak:
 * - Số ngày liên tiếp gần nhất mà user đã hoàn thành habit.
 *
 * longestStreak:
 * - Chuỗi ngày liên tiếp dài nhất kể từ khi bắt đầu theo dõi habit.
 *
 * completionRateWeek / Month:
 * - Tỷ lệ (%) số ngày đã hoàn thành / số ngày cần hoàn thành trong tuần/tháng hiện tại.
 */
@Data
@Builder
public class StreakInfo {

    /** Chuỗi ngày liên tiếp hiện tại. */
    private int currentStreak;

    /** Chuỗi ngày liên tiếp dài nhất từ trước đến nay. */
    private int longestStreak;

    /** Tỷ lệ % hoàn thành trong tuần hiện tại. */
    private double completionRateWeek;

    /** Tỷ lệ % hoàn thành trong tháng hiện tại. */
    private double completionRateMonth;
}