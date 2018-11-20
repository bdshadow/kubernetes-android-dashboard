package bdshadow.org.kubernetes.android.dashboard.exception;

public class SecureStoreNotSupportedException extends Exception {

    public SecureStoreNotSupportedException(Throwable cause) {
        super("An exception occurred while trying to save your data securely", cause);
    }
}
