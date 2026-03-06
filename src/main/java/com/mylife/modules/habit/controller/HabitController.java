package com.mylife.modules.habit.controller;

import com.mylife.common.response.ApiResponse;
import com.mylife.modules.habit.dto.request.CheckInRequest;
import com.mylife.modules.habit.dto.request.CreateHabitRequest;
import com.mylife.modules.habit.dto.request.UpdateHabitRequest;
import com.mylife.modules.habit.dto.response.HabitLogResponse;
import com.mylife.modules.habit.dto.response.HabitResponse;
import com.mylife.modules.habit.dto.response.StreakInfo;
import com.mylife.modules.habit.service.HabitService;
import com.mylife.modules.user.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller cho module Habit (Thói quen).
 *
 * Nhiệm vụ:
 * - Quản lý CRUD thói quen cho user hiện tại
 * - Check-in / undo check-in cho một thói quen
 * - Cung cấp các API thống kê: streak, logs trong khoảng thời gian
 *
 * Lưu ý:
 * - Tất cả endpoint đều dùng {@link CustomUserDetails} từ Spring Security để lấy user hiện tại.
 * - Controller chỉ làm nhiệm vụ nhận request/validate cơ bản, chuyển xuống {@link HabitService} xử lý nghiệp vụ,
 *   sau đó wrap kết quả trong {@link ApiResponse}.
 */
@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    // ========== CRUD ==========

    /**
     * Tạo thói quen mới cho user hiện tại.
     *
     * Body: {@link CreateHabitRequest}
     * - name, type, frequencyType, startDate là bắt buộc (đã được @Valid trên DTO kiểm tra).
     *
     * @param currentUser user đang đăng nhập (lấy từ SecurityContext)
     * @param request     thông tin thói quen cần tạo
     * @param httpRequest dùng để lấy requestURI cho logging / response metadata
     * @return HabitResponse đã được tạo
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateHabitRequest request,
            HttpServletRequest httpRequest) {
        HabitResponse response = habitService.createHabit(currentUser.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo thói quen thành công", httpRequest.getRequestURI(), HttpStatus.CREATED.value()));
    }

    /**
     * Lấy danh sách tất cả thói quen của user hiện tại.
     *
     * @param currentUser user đang đăng nhập
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return danh sách HabitResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HabitResponse>>> getAllHabits(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest) {
        List<HabitResponse> habits = habitService.getAllHabits(currentUser.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(habits, "Lấy danh sách thói quen thành công", httpRequest.getRequestURI()));
    }

    /**
     * Lấy chi tiết một thói quen theo ID (thuộc về user hiện tại).
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID của habit
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return HabitResponse tương ứng
     */
    @GetMapping("/{habitId}")
    public ResponseEntity<ApiResponse<HabitResponse>> getHabit(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            HttpServletRequest httpRequest) {
        HabitResponse habit = habitService.getHabit(currentUser.getUser().getId(), habitId);
        return ResponseEntity.ok(ApiResponse.success(habit, "Lấy thông tin thói quen thành công", httpRequest.getRequestURI()));
    }

    /**
     * Cập nhật một thói quen.
     *
     * Body: {@link UpdateHabitRequest} (các field đều optional, null = không thay đổi).
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit cần cập nhật
     * @param request     thông tin cập nhật
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return HabitResponse sau khi cập nhật
     */
    @PutMapping("/{habitId}")
    public ResponseEntity<ApiResponse<HabitResponse>> updateHabit(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            @Valid @RequestBody UpdateHabitRequest request,
            HttpServletRequest httpRequest) {
        HabitResponse updated = habitService.updateHabit(currentUser.getUser().getId(), habitId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật thói quen thành công", httpRequest.getRequestURI()));
    }

    /**
     * Xóa một thói quen (và toàn bộ logs, streak liên quan).
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit cần xóa
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return ApiResponse thành công (không có body)
     */
    @DeleteMapping("/{habitId}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            HttpServletRequest httpRequest) {
        habitService.deleteHabit(currentUser.getUser().getId(), habitId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa thói quen thành công", httpRequest.getRequestURI()));
    }

    /**
     * Bật/tắt trạng thái tạm dừng (pause) của một thói quen.
     *
     * Khi habit bị pause, user sẽ không thể check-in cho habit đó.
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit cần toggle
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return ApiResponse thành công (không có body)
     */
    @PatchMapping("/{habitId}/pause")
    public ResponseEntity<ApiResponse<Void>> togglePause(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            HttpServletRequest httpRequest) {
        habitService.togglePause(currentUser.getUser().getId(), habitId);
        return ResponseEntity.ok(ApiResponse.success(null, "Thay đổi trạng thái tạm dừng thành công", httpRequest.getRequestURI()));
    }

    // ========== CHECK-IN ==========

    /**
     * Check-in (ghi nhận hoàn thành) cho một thói quen vào một ngày cụ thể.
     *
     * Body: {@link CheckInRequest}
     * - date: ngày check-in (hôm nay hoặc quá khứ, không được tương lai)
     * - completed: dùng cho habit BINARY
     * - value: dùng cho habit NUMERIC/DURATION
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit cần check-in
     * @param request     thông tin check-in
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return HabitLogResponse cho log vừa được tạo
     */
    @PostMapping("/{habitId}/checkin")
    public ResponseEntity<ApiResponse<HabitLogResponse>> checkIn(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            @Valid @RequestBody CheckInRequest request,
            HttpServletRequest httpRequest) {
        HabitLogResponse log = habitService.checkIn(currentUser.getUser().getId(), habitId, request);
        return ResponseEntity.ok(ApiResponse.success(log, "Check-in thành công", httpRequest.getRequestURI()));
    }

    /**
     * Hủy check-in cho một ngày (xóa HabitLog tương ứng).
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit
     * @param date        ngày cần hủy check-in
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return ApiResponse thành công (không có body)
     */
    @DeleteMapping("/{habitId}/checkin")
    public ResponseEntity<ApiResponse<Void>> undoCheckIn(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest httpRequest) {
        habitService.undoCheckIn(currentUser.getUser().getId(), habitId, date);
        return ResponseEntity.ok(ApiResponse.success(null, "Hoàn tác check-in thành công", httpRequest.getRequestURI()));
    }

    // ========== THỐNG KÊ ==========

    /**
     * Lấy thông tin streak của một thói quen.
     *
     * Trả về:
     * - currentStreak: số ngày liên tiếp hiện tại
     * - longestStreak: chuỗi ngày liên tiếp dài nhất
     * - completionRateWeek / Month: tỷ lệ hoàn thành tuần/tháng
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return StreakInfo
     */
    @GetMapping("/{habitId}/streak")
    public ResponseEntity<ApiResponse<StreakInfo>> getStreakInfo(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            HttpServletRequest httpRequest) {
        StreakInfo streak = habitService.getStreakInfo(currentUser.getUser().getId(), habitId);
        return ResponseEntity.ok(ApiResponse.success(streak, "Lấy thông tin streak thành công", httpRequest.getRequestURI()));
    }

    /**
     * Lấy danh sách logs trong một khoảng ngày.
     *
     * @param currentUser user đang đăng nhập
     * @param habitId     ID habit
     * @param from        ngày bắt đầu (ISO, ví dụ 2024-01-01)
     * @param to          ngày kết thúc (ISO)
     * @param httpRequest để trả về thông tin endpoint trong ApiResponse
     * @return danh sách HabitLogResponse trong khoảng [from, to]
     */
    @GetMapping("/{habitId}/logs")
    public ResponseEntity<ApiResponse<List<HabitLogResponse>>> getLogs(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID habitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest) {
        List<HabitLogResponse> logs = habitService.getLogs(currentUser.getUser().getId(), habitId, from, to);
        return ResponseEntity.ok(ApiResponse.success(logs, "Lấy logs thành công", httpRequest.getRequestURI()));
    }
}