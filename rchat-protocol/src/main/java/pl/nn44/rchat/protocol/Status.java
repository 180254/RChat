package pl.nn44.rchat.protocol;

public enum Status {

    OK,

    ERROR_NOT_LOGGED_IN,
    ERROR_BAD_PASSWORD,
    ERROR_BAD_SESSION_ID,
    ERROR_NOT_INSIDE,
    ERROR_NO_PERMISSION,
    ERROR_NO_USER,
    ERROR_BANNED,

    ERROR_BAD_SYNTAX
}
