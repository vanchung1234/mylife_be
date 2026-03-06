package com.mylife.modules.habit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylife.common.exception.BusinessException;
import com.mylife.common.exception.ErrorCode;
import com.mylife.modules.habit.dto.request.CheckInRequest;
import com.mylife.modules.habit.dto.request.CreateHabitRequest;
import com.mylife.modules.habit.dto.request.UpdateHabitRequest;
import com.mylife.modules.habit.dto.response.HabitLogResponse;
import com.mylife.modules.habit.dto.response.HabitResponse;
import com.mylife.modules.habit.dto.response.StreakInfo;
import com.mylife.modules.habit.entity.Habit;
import com.mylife.modules.habit.entity.HabitLog;
import com.mylife.modules.habit.entity.HabitStreak;
import com.mylife.modules.habit.entity.enums.FrequencyType;
import com.mylife.modules.habit.entity.enums.HabitType;
import com.mylife.modules.habit.repository.HabitLogRepository;
import com.mylife.modules.habit.repository.HabitRepository;
import com.mylife.modules.habit.repository.HabitStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý logic nghiệp vụ cho module Habit (Thói quen).
 * 
 * Module này quản lý:
 * - CRUD thói quen (tạo, đọc, cập nhật, xóa)
 * - Check-in thói quen (ghi nhận hoàn thành)
 * - Tính toán streak (chuỗi ngày liên tiếp)
 * - Thống kê tỷ lệ hoàn thành
 * 
 * Các khái niệm quan trọng:
 * - HabitType: Loại thói quen (BINARY: có/không, NUMERIC: số lượng, DURATION: thời gian)
 * - FrequencyType: Tần suất lặp lại (DAILY, WEEKLY_DAYS, MONTHLY_DATES, CUSTOM_INTERVAL)
 * - Streak: Chuỗi ngày liên tiếp hoàn thành thói quen
 * - HabitLog: Bản ghi check-in cho một ngày cụ thể
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitStreakRepository habitStreakRepository;
    private final ObjectMapper objectMapper;

    // ========== CRUD HABIT ==========

    /**
     * Tạo thói quen mới cho user.
     * 
     * Flow:
     * 1. Tạo entity Habit từ request
     * 2. Validate frequencyData (JSON) dựa trên frequencyType
     * 3. Lưu habit vào DB
     * 4. Tạo HabitStreak record ban đầu (streak = 0)
     * 5. Trả về HabitResponse
     * 
     * @param userId ID của user sở hữu habit
     * @param request DTO chứa thông tin habit cần tạo
     * @return HabitResponse với thông tin habit đã tạo
     */
    @Transactional
    public HabitResponse createHabit(UUID userId, CreateHabitRequest request) {
        // Tạo entity Habit từ request
        Habit habit = new Habit();
        habit.setUserId(userId);
        habit.setName(request.getName());
        habit.setDescription(request.getDescription());
        habit.setIcon(request.getIcon());
        habit.setColor(request.getColor());
        habit.setType(request.getType());
        habit.setGoal(request.getGoal());
        habit.setUnit(request.getUnit());
        habit.setFrequencyType(request.getFrequencyType());
        habit.setFrequencyData(request.getFrequencyData());
        habit.setStartDate(request.getStartDate());
        habit.setEndDate(request.getEndDate());
        habit.setReminderTime(request.getReminderTime());
        // Mặc định remindEndOfDay = false nếu không được cung cấp
        habit.setRemindEndOfDay(request.getRemindEndOfDay() != null ? request.getRemindEndOfDay() : false);

        // Validate frequencyData: kiểm tra JSON hợp lệ và cấu trúc đúng với frequencyType
        // Ví dụ: WEEKLY_DAYS cần có field "daysOfWeek" là mảng [1,3,5]
        validateFrequencyData(request.getFrequencyType(), request.getFrequencyData());

        // Lưu habit vào database
        Habit saved = habitRepository.save(habit);

        // Tạo HabitStreak record ban đầu cho habit này
        // Streak ban đầu = 0 vì chưa có log nào
        HabitStreak streak = new HabitStreak();
        streak.setHabit(saved);
        streak.setCurrentStreak(0);
        streak.setLongestStreak(0);
        habitStreakRepository.save(streak);

        log.info("Created habit: {} for user: {}", saved.getName(), userId);
        return mapToResponse(saved);
    }

    /**
     * Lấy thông tin một habit cụ thể của user.
     * 
     * @param userId ID của user
     * @param habitId ID của habit cần lấy
     * @return HabitResponse với thông tin habit (bao gồm streak hiện tại)
     */
    @Transactional(readOnly = true)
    public HabitResponse getHabit(UUID userId, UUID habitId) {
        Habit habit = findHabitByIdAndUser(habitId, userId);
        return mapToResponse(habit);
    }

    /**
     * Lấy danh sách tất cả habits của user.
     * 
     * @param userId ID của user
     * @return Danh sách HabitResponse
     */
    @Transactional(readOnly = true)
    public List<HabitResponse> getAllHabits(UUID userId) {
        // findByUserId với Pageable = null sẽ trả về tất cả records
        return habitRepository.findByUserId(userId, null).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật thông tin habit.
     * 
     * Logic: Chỉ cập nhật các field được cung cấp (không null).
     * Nếu frequencyData được cập nhật, sẽ validate lại.
     * 
     * @param userId ID của user (để đảm bảo user chỉ update habit của mình)
     * @param habitId ID của habit cần update
     * @param request DTO chứa các field cần update (null = không update)
     * @return HabitResponse với thông tin đã cập nhật
     */
    @Transactional
    public HabitResponse updateHabit(UUID userId, UUID habitId, UpdateHabitRequest request) {
        Habit habit = findHabitByIdAndUser(habitId, userId);

        // Chỉ cập nhật các field không null (partial update)
        if (request.getName() != null) habit.setName(request.getName());
        if (request.getDescription() != null) habit.setDescription(request.getDescription());
        if (request.getIcon() != null) habit.setIcon(request.getIcon());
        if (request.getColor() != null) habit.setColor(request.getColor());
        if (request.getGoal() != null) habit.setGoal(request.getGoal());
        if (request.getUnit() != null) habit.setUnit(request.getUnit());
        
        // Nếu frequencyData được cập nhật, validate lại
        if (request.getFrequencyData() != null) {
            validateFrequencyData(habit.getFrequencyType(), request.getFrequencyData());
            habit.setFrequencyData(request.getFrequencyData());
        }
        
        if (request.getEndDate() != null) habit.setEndDate(request.getEndDate());
        if (request.getPaused() != null) habit.setPaused(request.getPaused());
        if (request.getReminderTime() != null) habit.setReminderTime(request.getReminderTime());
        if (request.getRemindEndOfDay() != null) habit.setRemindEndOfDay(request.getRemindEndOfDay());

        Habit updated = habitRepository.save(habit);
        log.info("Updated habit: {}", habitId);
        return mapToResponse(updated);
    }

    /**
     * Xóa habit (cascade sẽ xóa cả logs và streak).
     * 
     * @param userId ID của user
     * @param habitId ID của habit cần xóa
     */
    @Transactional
    public void deleteHabit(UUID userId, UUID habitId) {
        Habit habit = findHabitByIdAndUser(habitId, userId);
        // Cascade delete: xóa habit sẽ tự động xóa logs và streak (do orphanRemoval = true)
        habitRepository.delete(habit);
        log.info("Deleted habit: {}", habitId);
    }

    /**
     * Bật/tắt tạm dừng habit (pause/unpause).
     * 
     * Khi habit bị pause, user không thể check-in.
     * 
     * @param userId ID của user
     * @param habitId ID của habit cần toggle pause
     */
    @Transactional
    public void togglePause(UUID userId, UUID habitId) {
        Habit habit = findHabitByIdAndUser(habitId, userId);
        habit.setPaused(!habit.getPaused());
        habitRepository.save(habit);
        log.info("Toggled pause for habit: {} (now {})", habitId, habit.getPaused());
    }

    // ========== CHECK-IN ==========

    /**
     * Check-in (ghi nhận hoàn thành) habit cho một ngày cụ thể.
     * 
     * Flow:
     * 1. Validate habit không bị pause
     * 2. Validate ngày check-in không phải tương lai
     * 3. Kiểm tra chưa check-in ngày này (unique constraint)
     * 4. Xử lý check-in quá khứ xa (>7 ngày) → đánh dấu makeup
     * 5. Tạo HabitLog dựa trên loại habit:
     *    - BINARY: cần field "completed" (true/false)
     *    - NUMERIC/DURATION: cần field "value", tự động tính "completed" dựa trên goal
     * 6. Lưu log và cập nhật streak
     * 
     * @param userId ID của user
     * @param habitId ID của habit
     * @param request DTO chứa thông tin check-in (date, completed/value, note)
     * @return HabitLogResponse với thông tin log đã tạo
     */
    @Transactional
    public HabitLogResponse checkIn(UUID userId, UUID habitId, CheckInRequest request) {
        Habit habit = findHabitByIdAndUser(habitId, userId);

        // Validate habit không bị pause
        if (habit.getPaused()) {
            throw new BusinessException(ErrorCode.HABIT_PAUSED, "Habit đang tạm dừng, không thể check-in");
        }

        LocalDate today = LocalDate.now();
        
        // Validate không cho phép check-in tương lai
        if (request.getDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Không thể check-in cho ngày trong tương lai");
        }

        // Kiểm tra đã check-in ngày này chưa (unique constraint: một habit chỉ có 1 log/ngày)
        if (habitLogRepository.existsByHabitIdAndLogDate(habitId, request.getDate())) {
            throw new BusinessException(ErrorCode.HABIT_ALREADY_CHECKED, "Bạn đã check-in ngày này rồi");
        }

        // Xử lý check-in quá khứ xa (>7 ngày)
        // Policy: Cho phép nhưng đánh dấu là "makeup" (bù) để phân biệt với check-in đúng hạn
        long daysDiff = ChronoUnit.DAYS.between(request.getDate(), today);
        if (daysDiff > 7 && !request.getIsMakeup()) {
            // Tự động đánh dấu makeup nếu check-in quá khứ xa
            request.setIsMakeup(true);
        }

        // Tạo HabitLog entity
        HabitLog habitLog = new HabitLog();
        habitLog.setHabit(habit);
        habitLog.setLogDate(request.getDate());
        habitLog.setNote(request.getNote());
        habitLog.setIsMakeup(request.getIsMakeup());

        // Xử lý khác nhau theo loại habit
        if (habit.getType() == HabitType.BINARY) {
            // BINARY: Chỉ cần completed (true/false)
            // Ví dụ: "Đã tập thể dục" → completed = true
            if (request.getCompleted() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Với habit binary, cần cung cấp completed");
            }
            habitLog.setCompleted(request.getCompleted());
            habitLog.setValue(null); // Binary không có giá trị số
        } else {
            // NUMERIC hoặc DURATION: Cần giá trị số
            // Ví dụ NUMERIC: "Uống 2 lít nước" → value = 2.0
            // Ví dụ DURATION: "Tập thể dục 30 phút" → value = 30.0
            if (request.getValue() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Với habit " + habit.getType() + ", cần cung cấp value");
            }
            
            // Tự động tính completed: nếu value >= goal thì completed = true
            // Nếu habit không có goal (goal = null), thì mặc định completed = true khi có value
            boolean completed = habit.getGoal() == null || request.getValue() >= habit.getGoal();
            habitLog.setCompleted(completed);
            habitLog.setValue(request.getValue());
        }

        // Lưu log vào database
        HabitLog saved = habitLogRepository.save(habitLog);

        // Cập nhật streak dựa trên log mới này
        // Streak chỉ tăng khi completed = true và ngày liên tiếp
        updateStreak(habit, saved);

        log.info("Check-in for habit: {}, date: {}", habitId, request.getDate());
        return mapToLogResponse(saved);
    }

    /**
     * Hủy check-in (xóa log) cho một ngày cụ thể.
     * 
     * Sau khi xóa log, cần tính lại streak từ đầu vì streak có thể bị ảnh hưởng.
     * 
     * @param userId ID của user
     * @param habitId ID của habit
     * @param date Ngày cần hủy check-in
     */
    @Transactional
    public void undoCheckIn(UUID userId, UUID habitId, LocalDate date) {
        Habit habit = findHabitByIdAndUser(habitId, userId);
        
        // Tìm log cần xóa
        HabitLog habitLog = habitLogRepository.findByHabitIdAndLogDate(habitId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_LOG_NOT_FOUND));

        // Xóa log
        habitLogRepository.delete(habitLog);

        // Tính lại streak từ đầu vì việc xóa log có thể làm thay đổi streak
        // Ví dụ: Nếu xóa log ở giữa chuỗi, streak sẽ bị cắt đôi
        recalcStreak(habit);

        log.info("Undo check-in for habit: {}, date: {}", habitId, date);
    }

    // ========== THỐNG KÊ ==========

    /**
     * Lấy thông tin streak và tỷ lệ hoàn thành của habit.
     * 
     * Trả về:
     * - currentStreak: Chuỗi ngày liên tiếp hiện tại
     * - longestStreak: Chuỗi ngày liên tiếp dài nhất từ trước đến nay
     * - completionRateWeek: Tỷ lệ hoàn thành trong tuần này (%)
     * - completionRateMonth: Tỷ lệ hoàn thành trong tháng này (%)
     * 
     * Logic tính completion rate:
     * - completedDays: Số ngày đã check-in và completed = true trong khoảng thời gian
     * - requiredDays: Số ngày cần check-in trong khoảng (dựa trên frequencyType)
     * - rate = (completedDays / requiredDays) * 100
     * 
     * @param userId ID của user
     * @param habitId ID của habit
     * @return StreakInfo với các thống kê
     */
    @Transactional(readOnly = true)
    public StreakInfo getStreakInfo(UUID userId, UUID habitId) {
        Habit habit = findHabitByIdAndUser(habitId, userId);
        HabitStreak streak = habitStreakRepository.findByHabitId(habitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_NOT_FOUND));

        LocalDate today = LocalDate.now();
        
        // Tính khoảng thời gian tuần này (Monday -> Sunday)
        // getDayOfWeek().getValue() trả về: Monday=1, Sunday=7
        // minusDays(...) để lùi về Monday
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        // Tính khoảng thời gian tháng này (ngày 1 -> ngày cuối tháng)
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // Đếm số ngày đã hoàn thành (completed = true) trong tuần/tháng
        long completedWeek = habitLogRepository.countCompletedInPeriod(habitId, startOfWeek, endOfWeek);
        long completedMonth = habitLogRepository.countCompletedInPeriod(habitId, startOfMonth, endOfMonth);

        // Tính số ngày cần check-in trong tuần/tháng dựa trên frequencyType
        // Ví dụ: DAILY → 7 ngày/tuần, WEEKLY_DAYS [1,3,5] → 3 ngày/tuần
        long requiredWeek = countRequiredDaysInPeriod(habit, startOfWeek, endOfWeek);
        long requiredMonth = countRequiredDaysInPeriod(habit, startOfMonth, endOfMonth);

        // Tính tỷ lệ hoàn thành (%)
        // Tránh chia cho 0
        double rateWeek = requiredWeek == 0 ? 0 : (double) completedWeek / requiredWeek * 100;
        double rateMonth = requiredMonth == 0 ? 0 : (double) completedMonth / requiredMonth * 100;

        return StreakInfo.builder()
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .completionRateWeek(rateWeek)
                .completionRateMonth(rateMonth)
                .build();
    }

    /**
     * Lấy danh sách logs trong khoảng thời gian.
     * 
     * @param userId ID của user
     * @param habitId ID của habit
     * @param from Ngày bắt đầu (inclusive)
     * @param to Ngày kết thúc (inclusive)
     * @return Danh sách HabitLogResponse
     */
    @Transactional(readOnly = true)
    public List<HabitLogResponse> getLogs(UUID userId, UUID habitId, LocalDate from, LocalDate to) {
        // Validate user có quyền truy cập habit này
        findHabitByIdAndUser(habitId, userId);
        
        // Lấy danh sách logs trong khoảng thời gian
        return habitLogRepository.findByHabitIdAndLogDateBetween(habitId, from, to).stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Tìm habit theo ID và userId (đảm bảo user chỉ truy cập habit của mình).
     * 
     * @param habitId ID của habit
     * @param userId ID của user
     * @return Habit entity
     * @throws BusinessException nếu không tìm thấy
     */
    private Habit findHabitByIdAndUser(UUID habitId, UUID userId) {
        return habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_NOT_FOUND));
    }

    /**
     * Map Habit entity sang HabitResponse DTO.
     * 
     * Bao gồm cả thông tin streak (currentStreak, longestStreak).
     * 
     * @param habit Habit entity
     * @return HabitResponse DTO
     */
    private HabitResponse mapToResponse(Habit habit) {
        // Lấy streak của habit (có thể null nếu chưa có log nào)
        HabitStreak streak = habitStreakRepository.findByHabitId(habit.getId()).orElse(null);
        
        return HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .description(habit.getDescription())
                .icon(habit.getIcon())
                .color(habit.getColor())
                .type(habit.getType())
                .goal(habit.getGoal())
                .unit(habit.getUnit())
                .frequencyType(habit.getFrequencyType())
                .frequencyData(habit.getFrequencyData())
                .startDate(habit.getStartDate())
                .endDate(habit.getEndDate())
                .paused(habit.getPaused())
                .reminderTime(habit.getReminderTime())
                .remindEndOfDay(habit.getRemindEndOfDay())
                // Streak mặc định = 0 nếu chưa có record
                .currentStreak(streak != null ? streak.getCurrentStreak() : 0)
                .longestStreak(streak != null ? streak.getLongestStreak() : 0)
                .build();
    }

    /**
     * Map HabitLog entity sang HabitLogResponse DTO.
     * 
     * @param log HabitLog entity
     * @return HabitLogResponse DTO
     */
    private HabitLogResponse mapToLogResponse(HabitLog log) {
        return HabitLogResponse.builder()
                .id(log.getId())
                .logDate(log.getLogDate())
                .completed(log.getCompleted())
                .value(log.getValue())
                .note(log.getNote())
                .isMakeup(log.getIsMakeup())
                .build();
    }

    /**
     * Validate frequencyData (JSON string) dựa trên frequencyType.
     * 
     * Các loại frequencyType và cấu trúc JSON tương ứng:
     * 
     * 1. DAILY: Không cần frequencyData (hoặc null)
     *    → Check-in mỗi ngày
     * 
     * 2. WEEKLY_DAYS: Cần field "daysOfWeek" là mảng số [1-7]
     *    → Check-in vào các ngày cụ thể trong tuần
     *    → Ví dụ: {"daysOfWeek": [1,3,5]} = Monday, Wednesday, Friday
     *    → 1=Monday, 2=Tuesday, ..., 7=Sunday
     * 
     * 3. MONTHLY_DATES: Cần field "daysOfMonth" là mảng số [1-31]
     *    → Check-in vào các ngày cụ thể trong tháng
     *    → Ví dụ: {"daysOfMonth": [1,15,30]} = ngày 1, 15, 30 mỗi tháng
     * 
     * 4. CUSTOM_INTERVAL: Cần field "intervalDays" là số nguyên > 0
     *    → Check-in mỗi X ngày (chu kỳ)
     *    → Ví dụ: {"intervalDays": 2} = mỗi 2 ngày một lần
     * 
     * @param type Loại frequency (DAILY, WEEKLY_DAYS, MONTHLY_DATES, CUSTOM_INTERVAL)
     * @param data JSON string chứa dữ liệu frequency
     * @throws BusinessException nếu JSON không hợp lệ hoặc thiếu field bắt buộc
     */
    private void validateFrequencyData(FrequencyType type, String data) {
        // DAILY không cần frequencyData, có thể null hoặc empty
        if (type == FrequencyType.DAILY) {
            return; // Không cần validate
        }
        
        // Các loại khác đều cần frequencyData
        if (data == null || data.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "frequencyData không được để trống");
        }
        
        try {
            JsonNode node = objectMapper.readTree(data);
            
            switch (type) {
                case WEEKLY_DAYS:
                    // Kiểm tra có field "daysOfWeek" là mảng
                    if (!node.has("daysOfWeek") || !node.get("daysOfWeek").isArray()) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                                "WEEKLY_DAYS cần có trường daysOfWeek là mảng số từ 1-7");
                    }
                    // TODO: Có thể validate thêm: các số trong mảng phải từ 1-7
                    break;
                    
                case MONTHLY_DATES:
                    // Kiểm tra có field "daysOfMonth" là mảng
                    if (!node.has("daysOfMonth") || !node.get("daysOfMonth").isArray()) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                                "MONTHLY_DATES cần có trường daysOfMonth là mảng số từ 1-31");
                    }
                    // TODO: Có thể validate thêm: các số trong mảng phải từ 1-31
                    break;
                    
                case CUSTOM_INTERVAL:
                    // Kiểm tra có field "intervalDays" là số nguyên
                    if (!node.has("intervalDays") || !node.get("intervalDays").isInt()) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                                "CUSTOM_INTERVAL cần có trường intervalDays là số nguyên dương");
                    }
                    // TODO: Có thể validate thêm: intervalDays > 0
                    break;
                    
                default:
                    // DAILY đã được xử lý ở trên
                    break;
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "frequencyData không phải JSON hợp lệ: " + e.getMessage());
        }
    }

    /**
     * Cập nhật streak khi có log mới được tạo.
     * 
     * Logic tính streak:
     * - Streak chỉ tăng khi completed = true (hoàn thành)
     * - Streak chỉ tính các ngày liên tiếp (consecutive days)
     * - Nếu ngày log cách ngày log cuối > 1 ngày → streak bị reset về 1
     * - Nếu ngày log = ngày log cuối + 1 ngày → streak tăng lên
     * 
     * Ví dụ:
     * - Log ngày 1 (completed=true) → streak = 1
     * - Log ngày 2 (completed=true) → streak = 2 (liên tiếp)
     * - Log ngày 4 (completed=true) → streak = 1 (bị gián đoạn, reset)
     * - Log ngày 5 (completed=false) → không cập nhật streak (giữ nguyên = 1)
     * 
     * Policy về completed = false:
     * - Hiện tại: Chỉ cập nhật streak khi completed = true
     * - Nếu muốn reset streak khi completed = false, có thể sửa logic ở đây
     * 
     * @param habit Habit entity
     * @param log HabitLog mới được tạo
     */
    private void updateStreak(Habit habit, HabitLog log) {
        // Chỉ cập nhật streak khi log có completed = true
        // Nếu completed = false, streak không thay đổi (giữ nguyên)
        // Policy này có thể thay đổi: nếu muốn reset streak khi completed = false,
        // thì có thể set currentStreak = 0 ở đây
        if (!log.getCompleted()) {
            return;
        }

        // Lấy hoặc tạo HabitStreak record
        HabitStreak streak = habitStreakRepository.findByHabitId(habit.getId())
                .orElseGet(() -> {
                    // Nếu chưa có streak record, tạo mới
                    HabitStreak newStreak = new HabitStreak();
                    newStreak.setHabit(habit);
                    newStreak.setCurrentStreak(0);
                    newStreak.setLongestStreak(0);
                    return newStreak;
                });

        LocalDate lastLogDate = streak.getLastLogDate(); // Ngày log cuối cùng (completed = true)
        LocalDate currentLogDate = log.getLogDate(); // Ngày log hiện tại

        if (lastLogDate == null) {
            // Trường hợp đầu tiên: Chưa có log nào trước đó
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
        } else {
            // Kiểm tra xem ngày log hiện tại có liên tiếp với ngày log cuối không
            LocalDate expectedNextDate = lastLogDate.plusDays(1);
            
            if (currentLogDate.isEqual(expectedNextDate)) {
                // Liên tiếp: Ngày log hiện tại = ngày log cuối + 1 ngày
                // → Tăng streak lên
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                
                // Cập nhật longestStreak nếu currentStreak vượt qua
                if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                    streak.setLongestStreak(streak.getCurrentStreak());
                }
            } else if (currentLogDate.isAfter(expectedNextDate)) {
                // Bị gián đoạn: Ngày log hiện tại cách ngày log cuối > 1 ngày
                // → Reset streak về 1 (bắt đầu chuỗi mới)
                streak.setCurrentStreak(1);
            }
            // Trường hợp currentLogDate <= lastLogDate không xảy ra vì:
            // - Unique constraint: một habit chỉ có 1 log/ngày
            // - Log này mới được tạo, chưa tồn tại trong DB
        }

        // Cập nhật lastLogDate = ngày log hiện tại
        streak.setLastLogDate(currentLogDate);
        habitStreakRepository.save(streak);
    }

    /**
     * Tính lại streak từ đầu dựa trên tất cả logs hiện có.
     * 
     * Method này được gọi khi:
     * - Undo check-in (xóa log) → cần tính lại streak
     * - Có thể gọi để đảm bảo streak chính xác sau khi có thay đổi dữ liệu
     * 
     * Logic:
     * 1. Lấy tất cả logs (completed = true) sắp xếp theo ngày DESC (mới nhất trước)
     * 2. Duyệt từ mới nhất → cũ nhất để tính currentStreak (chuỗi hiện tại)
     * 3. Trong quá trình duyệt, track longestStreak (chuỗi dài nhất từ trước đến nay)
     * 
     * Ví dụ logs (sắp xếp DESC): [2024-01-05, 2024-01-04, 2024-01-02, 2024-01-01]
     * - Duyệt từ 2024-01-05:
     *   - 2024-01-05: currentStreak = 1 (ngày đầu tiên)
     *   - 2024-01-04: liên tiếp với 2024-01-05? → currentStreak = 2
     *   - 2024-01-02: không liên tiếp với 2024-01-04 → reset currentStreak = 1
     *   - 2024-01-01: liên tiếp với 2024-01-02? → currentStreak = 2
     * - Kết quả: currentStreak = 2 (từ 2024-01-01 đến 2024-01-02), longestStreak = 2
     * 
     * Lưu ý: Vì logs được sắp xếp DESC, ta duyệt từ mới → cũ, nên logic tính streak
     * phải ngược lại: kiểm tra prevDate - 1 = logDate (vì prevDate > logDate)
     * 
     * @param habit Habit entity cần tính lại streak
     */
    private void recalcStreak(Habit habit) {
        // Lấy tất cả logs (completed = true) sắp xếp theo ngày DESC (mới nhất trước)
        List<HabitLog> logs = habitLogRepository.findByHabitIdOrderByLogDateDesc(habit.getId())
                .stream()
                .filter(HabitLog::getCompleted) // Chỉ tính logs đã hoàn thành
                .collect(Collectors.toList());

        int currentStreak = 0; // Chuỗi ngày liên tiếp hiện tại (tính từ mới nhất)
        int longestStreak = 0; // Chuỗi ngày liên tiếp dài nhất từ trước đến nay
        LocalDate prevDate = null; // Ngày log trước đó (trong vòng lặp)

        // Duyệt từ mới nhất → cũ nhất
        for (HabitLog log : logs) {
            LocalDate logDate = log.getLogDate();
            
            if (prevDate == null) {
                // Log đầu tiên (mới nhất) → bắt đầu streak = 1
                currentStreak = 1;
            } else {
                // Kiểm tra xem logDate có liên tiếp với prevDate không
                // Vì sắp xếp DESC: prevDate > logDate
                // Liên tiếp nếu: prevDate - 1 ngày = logDate
                if (prevDate.minusDays(1).equals(logDate)) {
                    // Liên tiếp → tăng streak
                    currentStreak++;
                } else {
                    // Bị gián đoạn → reset streak về 1
                    currentStreak = 1;
                }
            }
            
            // Cập nhật longestStreak nếu currentStreak vượt qua
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak;
            }
            
            prevDate = logDate;
        }

        // Lấy hoặc tạo HabitStreak record
        HabitStreak streak = habitStreakRepository.findByHabitId(habit.getId())
                .orElse(new HabitStreak());
        streak.setHabit(habit);
        streak.setCurrentStreak(currentStreak);
        streak.setLongestStreak(longestStreak);
        // lastLogDate = ngày log mới nhất (phần tử đầu tiên trong list DESC)
        streak.setLastLogDate(logs.isEmpty() ? null : logs.get(0).getLogDate());
        habitStreakRepository.save(streak);
    }

    /**
     * Tính số ngày cần check-in trong khoảng thời gian dựa trên frequencyType và frequencyData.
     * 
     * Logic theo từng loại frequencyType:
     * 
     * 1. DAILY: Mỗi ngày trong khoảng
     *    → Số ngày = tổng số ngày từ start đến end
     * 
     * 2. WEEKLY_DAYS: Các ngày cụ thể trong tuần (ví dụ: [1,3,5] = Mon, Wed, Fri)
     *    → Đếm số ngày trong khoảng mà thuộc các ngày trong tuần được chỉ định
     *    → Ví dụ: Khoảng 1 tuần với daysOfWeek=[1,3,5] → 3 ngày
     * 
     * 3. MONTHLY_DATES: Các ngày cụ thể trong tháng (ví dụ: [1,15,30])
     *    → Đếm số ngày trong khoảng mà ngày trong tháng thuộc danh sách
     *    → Ví dụ: Khoảng 1 tháng với daysOfMonth=[1,15,30] → 3 ngày
     * 
     * 4. CUSTOM_INTERVAL: Mỗi X ngày một lần (ví dụ: intervalDays=2)
     *    → Đếm số ngày trong khoảng mà cách nhau đúng intervalDays
     *    → Ví dụ: Khoảng 7 ngày với intervalDays=2 → 4 ngày (ngày 1, 3, 5, 7)
     * 
     * @param habit Habit entity (chứa frequencyType và frequencyData)
     * @param start Ngày bắt đầu (inclusive)
     * @param end Ngày kết thúc (inclusive)
     * @return Số ngày cần check-in trong khoảng thời gian
     */
    private long countRequiredDaysInPeriod(Habit habit, LocalDate start, LocalDate end) {
        FrequencyType frequencyType = habit.getFrequencyType();
        String frequencyData = habit.getFrequencyData();
        
        switch (frequencyType) {
            case DAILY:
                // DAILY: Mỗi ngày trong khoảng
                return ChronoUnit.DAYS.between(start, end) + 1;
                
            case WEEKLY_DAYS:
                // WEEKLY_DAYS: Các ngày cụ thể trong tuần
                // frequencyData = {"daysOfWeek": [1,3,5]} (1=Monday, 7=Sunday)
                try {
                    JsonNode node = objectMapper.readTree(frequencyData);
                    JsonNode daysOfWeekNode = node.get("daysOfWeek");
                    
                    if (daysOfWeekNode == null || !daysOfWeekNode.isArray()) {
                        // Fallback: nếu không parse được, trả về tổng số ngày
                        return ChronoUnit.DAYS.between(start, end) + 1;
                    }
                    
                    // Lấy danh sách các ngày trong tuần cần check-in
                    List<Integer> daysOfWeek = new ArrayList<>();
                    for (JsonNode dayNode : daysOfWeekNode) {
                        if (dayNode.isInt()) {
                            daysOfWeek.add(dayNode.asInt());
                        }
                    }
                    
                    // Đếm số ngày trong khoảng mà thuộc các ngày trong tuần được chỉ định
                    long count = 0;
                    LocalDate current = start;
                    while (!current.isAfter(end)) {
                        // getDayOfWeek().getValue() trả về: Monday=1, Sunday=7
                        int dayOfWeek = current.getDayOfWeek().getValue();
                        if (daysOfWeek.contains(dayOfWeek)) {
                            count++;
                        }
                        current = current.plusDays(1);
                    }
                    return count;
                } catch (Exception e) {
                    log.warn("Error parsing frequencyData for WEEKLY_DAYS: {}", e.getMessage());
                    // Fallback: trả về tổng số ngày
                    return ChronoUnit.DAYS.between(start, end) + 1;
                }
                
            case MONTHLY_DATES:
                // MONTHLY_DATES: Các ngày cụ thể trong tháng
                // frequencyData = {"daysOfMonth": [1,15,30]}
                try {
                    JsonNode node = objectMapper.readTree(frequencyData);
                    JsonNode daysOfMonthNode = node.get("daysOfMonth");
                    
                    if (daysOfMonthNode == null || !daysOfMonthNode.isArray()) {
                        return ChronoUnit.DAYS.between(start, end) + 1;
                    }
                    
                    List<Integer> daysOfMonth = new ArrayList<>();
                    for (JsonNode dayNode : daysOfMonthNode) {
                        if (dayNode.isInt()) {
                            daysOfMonth.add(dayNode.asInt());
                        }
                    }
                    
                    // Đếm số ngày trong khoảng mà ngày trong tháng thuộc danh sách
                    long count = 0;
                    LocalDate current = start;
                    while (!current.isAfter(end)) {
                        int dayOfMonth = current.getDayOfMonth();
                        if (daysOfMonth.contains(dayOfMonth)) {
                            count++;
                        }
                        current = current.plusDays(1);
                    }
                    return count;
                } catch (Exception e) {
                    log.warn("Error parsing frequencyData for MONTHLY_DATES: {}", e.getMessage());
                    return ChronoUnit.DAYS.between(start, end) + 1;
                }
                
            case CUSTOM_INTERVAL:
                // CUSTOM_INTERVAL: Mỗi X ngày một lần
                // frequencyData = {"intervalDays": 2}
                try {
                    JsonNode node = objectMapper.readTree(frequencyData);
                    JsonNode intervalDaysNode = node.get("intervalDays");
                    
                    if (intervalDaysNode == null || !intervalDaysNode.isInt()) {
                        return ChronoUnit.DAYS.between(start, end) + 1;
                    }
                    
                    int intervalDays = intervalDaysNode.asInt();
                    if (intervalDays <= 0) {
                        return ChronoUnit.DAYS.between(start, end) + 1;
                    }
                    
                    // Đếm số ngày trong khoảng mà cách nhau đúng intervalDays
                    // Bắt đầu từ start, mỗi intervalDays ngày một lần
                    long count = 0;
                    LocalDate current = start;
                    while (!current.isAfter(end)) {
                        count++;
                        current = current.plusDays(intervalDays);
                    }
                    return count;
                } catch (Exception e) {
                    log.warn("Error parsing frequencyData for CUSTOM_INTERVAL: {}", e.getMessage());
                    return ChronoUnit.DAYS.between(start, end) + 1;
                }
                
            default:
                // Fallback: trả về tổng số ngày
                return ChronoUnit.DAYS.between(start, end) + 1;
        }
    }
}