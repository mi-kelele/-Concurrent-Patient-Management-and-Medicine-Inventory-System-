package Problem2;

/** Unchecked wrapper so worker threads' run() methods don't need throws clauses. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
