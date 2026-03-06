package com.mylife.modules.habit.entity.enums;

/**
 * Loại tần suất lặp lại:
 * - DAILY: hàng ngày
 * - WEEKLY_DAYS: các ngày cụ thể trong tuần (ví dụ: thứ 2,4,6)
 * - MONTHLY_DATES: các ngày cụ thể trong tháng (1,15,30)
 * - CUSTOM_INTERVAL: mỗi X ngày (chu kỳ)
 */
public enum FrequencyType {
    DAILY,
    WEEKLY_DAYS,
    MONTHLY_DATES,
    CUSTOM_INTERVAL
}