package com.mylife.modules.habit.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO trả về cho một bản ghi HabitLog.
 *
 * Được dùng trong:
 * - API check-in (trả về log vừa tạo)
 * - API lấy danh sách logs trong khoảng thời gian
 */
@Data
@Builder
public class HabitLogResponse {

    /** ID của log (UUID). */
    private UUID id;

    /** Ngày check-in. */
    private LocalDate logDate;

    /** Đã hoàn thành hay chưa. */
    private Boolean completed;

    /** Giá trị đạt được (cho NUMERIC/DURATION). */
    private Double value;

    /** Ghi chú. */
    private String note;

    /** Có phải là check-in bù (makeup) không. */
    private Boolean isMakeup;
}