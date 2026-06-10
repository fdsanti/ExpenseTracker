package com.example.expensetracker.ui.expense;

import androidx.annotation.Nullable;

import com.example.expensetracker.model.Tracker;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class TrackerDateUtils {

    private TrackerDateUtils() {
    }

    public static int getTrackerDay(@Nullable Tracker tracker) {
        if (tracker == null || tracker.getCreatedAt() <= 0L) {
            return 0;
        }

        long start = startOfDay(tracker.getCreatedAt());
        long today = startOfDay(System.currentTimeMillis());

        if (today < start) {
            return 1;
        }

        long diffMillis = today - start;
        return (int) TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;
    }

    public static boolean shouldShowClosingVariant(@Nullable Tracker tracker) {
        return tracker != null
                && !tracker.isClosed()
                && getTrackerDay(tracker) > 20
                && isAfterFirstTwentyDaysDominantMonth(tracker.getCreatedAt(), System.currentTimeMillis());
    }

    private static long startOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static boolean isAfterFirstTwentyDaysDominantMonth(long createdAtMillis, long currentMillis) {
        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(startOfDay(createdAtMillis));

        int firstMonthYear = monthYear(cursor);
        int firstMonthCount = 0;
        int secondMonthYear = firstMonthYear;
        int secondMonthCount = 0;

        for (int i = 0; i < 20; i++) {
            int currentMonthYear = monthYear(cursor);
            if (currentMonthYear == firstMonthYear) {
                firstMonthCount++;
            } else {
                if (secondMonthCount == 0) {
                    secondMonthYear = currentMonthYear;
                }
                secondMonthCount++;
            }

            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        int dominantMonthYear = firstMonthCount >= secondMonthCount
                ? firstMonthYear
                : secondMonthYear;

        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(currentMillis);

        return monthYear(today) > dominantMonthYear;
    }

    private static int monthYear(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH);
    }
}
