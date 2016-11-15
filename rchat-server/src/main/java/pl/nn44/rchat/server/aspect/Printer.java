package pl.nn44.rchat.server.aspect;

@FunctionalInterface
interface Printer {

    void log(String format, Object... arguments);
}
