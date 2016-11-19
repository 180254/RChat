package pl.nn44.rchat.server.aspect;

/**
 * Functional interface for org.slf4j.Logger log methods.<br/>
 * It is to make processing easier.
 */
@FunctionalInterface
interface Printer {

    void log(String format, Object... arguments);
}
