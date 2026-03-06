package com.mylife.modules.habit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO request cho API check-in habit.
 *
 * Tùy vào loại habit:
 * - BINARY: dùng field {@code completed}
 * - NUMERIC/DURATION: dùng field {@code value}
 *
 * Các field:
 * - date: ngày check-in (bắt buộc, không được là tương lai)
 * - completed: dành cho habit BINARY
 * - value: dành cho habit NUMERIC/DURATION
 * - note: ghi chú tùy ý
 * - isMakeup: đánh dấu đây là check-in bù cho quá khứ (có thể được service set tự động)
 */
@Data
public class CheckInRequest {

    /**
     * Ngày check-in.
     * - Có thể là hôm nay hoặc một ngày trong quá khứ.
     * - Không được là ngày trong tương lai (được validate ở service).
     */
    @NotNull
    private LocalDate date;

    /**
     * Trạng thái hoàn thành cho habit BINARY.
     * - true: đã hoàn thành trong ngày
     * - false: không hoàn thành
     * - null: không áp dụng (NUMERIC/DURATION)
     */
    private Boolean completed;

    /**
     * Giá trị đạt được cho habit NUMERIC/DURATION.
     * - Ví dụ NUMERIC: số cốc nước đã uống
     * - Ví dụ DURATION: số phút đã tập thể dục
     */
    private Double value;

    /**
     * Ghi chú thêm cho log (tùy chọn).
     */
    private String note;

    /**
     * Đánh dấu đây là check-in bù (makeup) cho quá khứ.
     * - Nếu user check-in cho ngày cách hiện tại quá xa, service có thể tự set flag này.
     */
    private Boolean isMakeup = false;
}