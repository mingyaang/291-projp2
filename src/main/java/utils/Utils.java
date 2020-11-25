package utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String generateID(int count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static boolean stringContains(String key, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(key);
        return m.find();
    }
}
