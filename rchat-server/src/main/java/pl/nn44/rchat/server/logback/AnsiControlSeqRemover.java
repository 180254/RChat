package pl.nn44.rchat.server.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.regex.Pattern;

public class AnsiControlSeqRemover extends CompositeConverter<ILoggingEvent> {

    private static final Pattern ANSI_CONTROL_SEQ =
            /*
             *           regexp credits info
             * credits: ninjalj @ stackoverflow.com
             * url: http://stackoverflow.com/a/25189932
             * licensed: cc by-sa 3.0 with attribution required
             * license url: https://creativecommons.org/licenses/by-sa/3.0/
             */
            Pattern.compile("(\\x1b\\x5b|\\x9b)[\\x30-\\x3f]*[\\x20-\\x2f]*[\\x40-\\x7e]");

    private static final String REPLACEMENT =
            "";

    @Override
    protected String transform(ILoggingEvent event, String in) {
        return ANSI_CONTROL_SEQ.matcher(in).replaceAll(REPLACEMENT);
    }
}
