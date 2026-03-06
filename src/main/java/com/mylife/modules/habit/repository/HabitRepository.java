package com.mylife.modules.habit.repository;

import com.mylife.modules.habit.entity.Habit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity {@link Habit}.
 *
 * Cung cấp các method truy vấn thói quen theo userId, trạng thái, và phục vụ tính năng nhắc nhở.
 */
@Repository
public interface HabitRepository extends JpaRepository<Habit, UUID> {

    /**
     * Lấy danh sách habits của một user với phân trang.
     *
     * @param userId   ID user
     * @param pageable thông tin phân trang (page, size, sort)
     * @return page các Habit
     */
    Page<Habit> findByUserId(UUID userId, Pageable pageable);

    /**
     * Tìm một habit theo id và userId (đảm bảo user chỉ truy cập habit của mình).
     *
     * @param id     ID habit
     * @param userId ID user
     * @return Optional Habit
     */
    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Lấy danh sách habits đang active (không bị pause) của user.
     *
     * @param userId ID user
     * @return danh sách Habit
     */
    List<Habit> findByUserIdAndPausedFalse(UUID userId);

    /**
     * Tìm các habits cần được nhắc ở một thời điểm cụ thể trong ngày (tất cả user).
     *
     * Điều kiện:
     * - paused = false
     * - reminderTime trùng với thời điểm hiện tại (theo phút)
     * - startDate <= today <= endDate (nếu có endDate)
     *
     * Lưu ý: logic frequency (DAILY/WEEKLY/...) sẽ được xử lý ở service.
     */
    @Query("""
           SELECT h FROM Habit h
           WHERE h.paused = false
             AND h.reminderTime = :time
             AND h.startDate <= :today
             AND (h.endDate IS NULL OR h.endDate >= :today)
           """)
    List<Habit> findHabitsToRemindAtTime(@Param("time") LocalTime time, @Param("today") LocalDate today);

    /**
     * Tìm các habits cần gửi nhắc cuối ngày (tất cả user):
     * - remindEndOfDay = true
     * - không bị pause
     * - today nằm trong khoảng [startDate, endDate]
     * - và hôm nay chưa có HabitLog nào (chưa check-in)
     */
    @Query("""
           SELECT h FROM Habit h
           WHERE h.remindEndOfDay = true
             AND h.paused = false
             AND h.startDate <= :today
             AND (h.endDate IS NULL OR h.endDate >= :today)
             AND NOT EXISTS (
                 SELECT 1 FROM HabitLog hl
                 WHERE hl.habit = h AND hl.logDate = :today
             )
           """)
    List<Habit> findHabitsNeedingEndOfDayReminder(@Param("today") LocalDate today);
}