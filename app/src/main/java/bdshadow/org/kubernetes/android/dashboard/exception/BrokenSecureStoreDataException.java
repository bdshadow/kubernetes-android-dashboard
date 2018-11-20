package bdshadow.org.kubernetes.android.dashboard.exception;

public class BrokenSecureStoreDataException extends Exception {
    public BrokenSecureStoreDataException() {
        super("An exception occurred while trying to restore encrypted data");
    }

    public BrokenSecureStoreDataException(Throwable t) {
        super("An exception occurred while trying to restore encrypted data", t);
    }
}
