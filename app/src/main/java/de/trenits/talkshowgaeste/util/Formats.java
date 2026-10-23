package de.trenits.talkshowgaeste.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class Formats {

    private static final String[] SHORT = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    private static final String[] LONG = {"Montag", "Dienstag", "Mittwoch", "Donnerstag",
            "Freitag", "Samstag", "Sonntag"};

    private static final DateTimeFormatter DAY_MONTH =
            DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN);
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN);
    private static final DateTimeFormatter COMPACT_DATE =
            DateTimeFormatter.ofPattern("EEE\ndd.MM.", Locale.GERMAN);

    private Formats() {
    }

    public static String weekdayShort(int isoDay) {
        if (isoDay < 1 || isoDay > 7) return "";
        return SHORT[isoDay - 1];
    }

    public static String weekdayLong(int isoDay) {
        if (isoDay < 1 || isoDay > 7) return "";
        return LONG[isoDay - 1];
    }

    public static String weekdays(List<Integer> days) {
        if (days == null || days.isEmpty()) return "Keine Sendetage";
        if (days.size() == 7) return "Täglich";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(weekdayShort(days.get(i)));
        }
        return builder.toString();
    }

    public static String dayMonth(LocalDate date) {
        return date.format(DAY_MONTH);
    }

    public static String shortDate(LocalDate date) {
        return date.format(SHORT_DATE);
    }

    public static String compactDate(LocalDate date) {
        return date.format(COMPACT_DATE);
    }

    public static boolean validTime(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        if (!trimmed.matches("^([01]?\\d|2[0-3]):[0-5]\\d$")) return false;
        return true;
    }

    public static String normalizeTime(String value) {
        String trimmed = value.trim();
        if (trimmed.length() == 4) trimmed = "0" + trimmed;
        return trimmed;
    }
}
