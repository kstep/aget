package me.kstep.aget;

import java.io.File;

class Util {
    public static String humanizeSize(String format, long value) {
        String[] suffixes = {"b", "K", "M", "G", "T"};
        int index = 0;
        float fvalue = value;

        while (fvalue > 1024 && index < suffixes.length) {
            fvalue /= 1024;
            index++;
        }

        return String.format(format, fvalue, suffixes[index]);
    }

    public static String humanizeSize(long value) {
        return humanizeSize("%3.2f%s", value);
    }

    public static String humanizeTime(long time) {
        if (time <= 0) {
            return "";
        }

        final long[] coeffs = {
            86400L*7, // weeks
            86400L, // days
            3600L, // hours
            60L, // minutes
            1L, // seconds
        };
        final String[] names = {
            "w", "d", "h", "m", "s",
        };

        int items = 0;
        long value = 0;
        String result = "";

        for (int i = 0; i < coeffs.length; i++) {
            value = time / coeffs[i];
            time %= coeffs[i];

            if (value != 0) {
                result += value + names[i];
                if (++items > 1) {
                    break;
                }
            }
        }

        return result;
    }
}

