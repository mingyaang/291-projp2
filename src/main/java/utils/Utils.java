package utils;

import org.apache.commons.lang3.RandomStringUtils;

public class Utils {
    public static String generateID(int count) {
        return RandomStringUtils.random(count, false, true);
    }
}
