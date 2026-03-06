package com.mylife.modules.habit.entity.enums;

/**
 * Loại thói quen:
 * - BINARY: hoàn thành / chưa hoàn thành (true/false)
 * - NUMERIC: theo số lượng (số lần, số lượng, lít...)
 * - DURATION: theo thời gian (phút, giờ)
 */
public enum HabitType {
    BINARY,
    NUMERIC,
    DURATION
}