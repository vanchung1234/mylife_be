package com.mylife.modules.habit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylife.modules.habit.entity.Habit;
import com.mylife.modules.habit.entity.enums.FrequencyType;
import com.mylife.modules.habit.repository.HabitRepository;
import com.mylife.modules.user.service.FcmNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Scheduler định kỳ để gửi nhắc nhở thói quen qua FCM:
 * - Mỗi phút: gửi nhắc cho các habits có reminderTime = giờ/phút hiện tại.
 * - Mỗi ngày 21:00: gửi nhắc cuối ngày cho các habits chưa được check-in (remindEndOfDay = true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HabitReminderScheduler {

    private final HabitRepository habitRepository;
    private final FcmNotificationService fcmNotificationService;
    private final ObjectMapper objectMapper;

    /**
     * Chạy mỗi phút để gửi nhắc nhở theo reminderTime của habit.
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendTimeBasedReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<Habit> habits = habitRepository.findHabitsToRemindAtTime(now, today);
        if (habits.isEmpty()) {
            return;
        }

        // Lọc theo frequencyType / frequencyData để chỉ gửi vào các ngày "phải" thực hiện habit
        List<Habit> activeHabits = habits.stream()
                .filter(habit -> isRequiredDayForHabit(habit, today))
                .toList();

        if (activeHabits.isEmpty()) {
            return;
        }

        // Gom nhóm theo userId để gửi 1 notification / user
        Map<UUID, List<Habit>> habitsByUser = activeHabits.stream()
                .collect(Collectors.groupingBy(Habit::getUserId));

        habitsByUser.forEach((userId, userHabits) -> {
            String title = "Nhắc nhở thói quen";
            String body;
            if (userHabits.size() == 1) {
                body = "Đến giờ thực hiện thói quen: " + userHabits.get(0).getName();
            } else {
                body = "Đến giờ thực hiện " + userHabits.size() + " thói quen. Hãy vào ứng dụng để check-in nhé!";
            }

            fcmNotificationService.sendNotificationToUser(
                    userId,
                    title,
                    body,
                    Map.of(
                            "type", "HABIT_REMINDER",
                            "timeBased", "true"
                    )
            );
        });

        log.debug("Sent time-based habit reminders at {} for {} habits", now, activeHabits.size());
    }

    /**
     * Chạy mỗi ngày lúc 21:00 để nhắc cuối ngày cho những habit chưa được check-in.
     * (Có thể điều chỉnh cron theo nhu cầu thực tế hoặc theo timezone của user).
     */
    @Scheduled(cron = "0 0 21 * * *")
    public void sendEndOfDayReminders() {
        LocalDate today = LocalDate.now();
        List<Habit> habits = habitRepository.findHabitsNeedingEndOfDayReminder(today);
        if (habits.isEmpty()) {
            return;
        }

        // Lọc theo frequency để tránh nhắc cho những ngày không yêu cầu thực hiện habit
        List<Habit> activeHabits = habits.stream()
                .filter(habit -> isRequiredDayForHabit(habit, today))
                .toList();

        if (activeHabits.isEmpty()) {
            return;
        }

        Map<UUID, List<Habit>> habitsByUser = activeHabits.stream()
                .collect(Collectors.groupingBy(Habit::getUserId));

        habitsByUser.forEach((userId, userHabits) -> {
            String title = "Nhắc cuối ngày";
            String body = "Bạn còn " + userHabits.size() + " thói quen chưa check-in hôm nay. Đừng quên hoàn thành nhé!";

            fcmNotificationService.sendNotificationToUser(
                    userId,
                    title,
                    body,
                    Map.of(
                            "type", "HABIT_REMINDER",
                            "timeBased", "false"
                    )
            );
        });

        log.debug("Sent end-of-day habit reminders for {} habits", activeHabits.size());
    }

    /**
     * Kiểm tra một ngày cụ thể có phải là ngày "phải" thực hiện habit không,
     * dựa trên frequencyType và frequencyData.
     */
    private boolean isRequiredDayForHabit(Habit habit, LocalDate date) {
        FrequencyType type = habit.getFrequencyType();
        String data = habit.getFrequencyData();

        try {
            return switch (type) {
                case DAILY -> true;
                case WEEKLY_DAYS -> isWeeklyDayRequired(data, date);
                case MONTHLY_DATES -> isMonthlyDateRequired(data, date);
                case CUSTOM_INTERVAL -> isCustomIntervalRequired(habit, data, date);
            };
        } catch (Exception e) {
            // Nếu parse JSON lỗi thì fallback: coi như ngày nào cũng required
            log.warn("Error parsing frequencyData for habit {}: {}. Fallback to required=true",
                    habit.getId(), e.getMessage());
            return true;
        }
    }

    private boolean isWeeklyDayRequired(String frequencyData, LocalDate date) throws Exception {
        if (frequencyData == null || frequencyData.isEmpty()) {
            return true;
        }
        JsonNode node = objectMapper.readTree(frequencyData);
        JsonNode daysOfWeekNode = node.get("daysOfWeek");
        if (daysOfWeekNode == null || !daysOfWeekNode.isArray()) {
            return true;
        }
        int dayOfWeek = date.getDayOfWeek().getValue(); // Monday=1 ... Sunday=7
        for (JsonNode dayNode : daysOfWeekNode) {
            if (dayNode.isInt() && dayNode.asInt() == dayOfWeek) {
                return true;
            }
        }
        return false;
    }

    private boolean isMonthlyDateRequired(String frequencyData, LocalDate date) throws Exception {
        if (frequencyData == null || frequencyData.isEmpty()) {
            return true;
        }
        JsonNode node = objectMapper.readTree(frequencyData);
        JsonNode daysOfMonthNode = node.get("daysOfMonth");
        if (daysOfMonthNode == null || !daysOfMonthNode.isArray()) {
            return true;
        }
        int dayOfMonth = date.getDayOfMonth();
        for (JsonNode dayNode : daysOfMonthNode) {
            if (dayNode.isInt() && dayNode.asInt() == dayOfMonth) {
                return true;
            }
        }
        return false;
    }

    private boolean isCustomIntervalRequired(Habit habit, String frequencyData, LocalDate date) throws Exception {
        if (frequencyData == null || frequencyData.isEmpty()) {
            return true;
        }
        JsonNode node = objectMapper.readTree(frequencyData);
        JsonNode intervalDaysNode = node.get("intervalDays");
        if (intervalDaysNode == null || !intervalDaysNode.isInt()) {
            return true;
        }
        int intervalDays = intervalDaysNode.asInt();
        if (intervalDays <= 0) {
            return true;
        }

        LocalDate startDate = habit.getStartDate();
        if (date.isBefore(startDate)) {
            return false;
        }

        long diff = ChronoUnit.DAYS.between(startDate, date);
        return diff % intervalDays == 0;
    }
}

