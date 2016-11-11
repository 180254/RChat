package pl.nn44.rchat.client.util;

import java.util.regex.Pattern;

public class Lf {

    private static final Pattern NL_PATTERN = Pattern.compile("\n");

    public static String r(String text) {
        return NL_PATTERN.matcher(text).replaceAll(" ");
    }
}
