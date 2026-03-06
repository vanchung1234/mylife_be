package com.mylife.modules.habit.repository;

import com.mylife.modules.habit.entity.HabitStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity {@link HabitStreak}.
 *
 * Chủ yếu dùng để lấy/cập nhật streak theo habitId.
 */
@Repository
public interface HabitStreakRepository extends JpaRepository<HabitStreak, UUID> {

    /**
     * Tìm HabitStreak theo ID của Habit.
     *
     * @param habitId ID habit
     * @return Optional HabitStreak
     */
    Optional<HabitStreak> findByHabitId(UUID habitId);
}