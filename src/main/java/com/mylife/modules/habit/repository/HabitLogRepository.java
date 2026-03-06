package com.mylife.modules.habit.repository;

import com.mylife.modules.habit.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity {@link HabitLog}.
 *
 * Cung cấp các method để:
 * - Tìm log theo habit + ngày
 * - Lấy toàn bộ logs của một habit
 * - Thống kê số ngày đã hoàn thành trong một khoảng thời gian
 */
@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, UUID> {

    /**
     * Tìm log theo habitId và logDate.
     *
     * @param habitId ID habit
     * @param logDate ngày log
     * @return Optional HabitLog
     */
    Optional<HabitLog> findByHabitIdAndLogDate(UUID habitId, LocalDate logDate);

    /**
     * Lấy tất cả logs của một habit, sắp xếp theo ngày DESC (mới nhất trước).
     *
     * @param habitId ID habit
     * @return danh sách HabitLog
     */
    List<HabitLog> findByHabitIdOrderByLogDateDesc(UUID habitId);

    /**
     * Lấy các logs của một habit trong khoảng ngày [start, end].
     *
     * @param habitId ID habit
     * @param start   ngày bắt đầu (inclusive)
     * @param end     ngày kết thúc (inclusive)
     * @return danh sách HabitLog
     */
    List<HabitLog> findByHabitIdAndLogDateBetween(UUID habitId, LocalDate start, LocalDate end);

    /**
     * Kiểm tra đã tồn tại log cho một habit tại một ngày cụ thể hay chưa.
     *
     * Dùng để đảm bảo:
     * - Một habit chỉ có tối đa 1 log/ngày.
     *
     * @param habitId ID habit
     * @param date    ngày cần kiểm tra
     * @return true nếu đã tồn tại log, false nếu chưa
     */
    @Query("SELECT COUNT(hl) > 0 FROM HabitLog hl WHERE hl.habit.id = :habitId AND hl.logDate = :date")
    boolean existsByHabitIdAndLogDate(@Param("habitId") UUID habitId, @Param("date") LocalDate date);

    /**
     * Đếm số ngày đã hoàn thành (completed = true) trong khoảng [start, end].
     *
     * Dùng để tính tỷ lệ hoàn thành tuần/tháng trong {@link com.mylife.modules.habit.service.HabitService}.
     *
     * @param habitId ID habit
     * @param start   ngày bắt đầu
     * @param end     ngày kết thúc
     * @return số logs completed trong khoảng
     */
    @Query("SELECT COUNT(hl) FROM HabitLog hl WHERE hl.habit.id = :habitId AND hl.logDate BETWEEN :start AND :end AND hl.completed = true")
    long countCompletedInPeriod(@Param("habitId") UUID habitId, @Param("start") LocalDate start,
                                @Param("end") LocalDate end);
}