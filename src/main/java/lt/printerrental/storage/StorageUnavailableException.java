package lt.printerrental.storage;

public class StorageUnavailableException extends RuntimeException {
  public StorageUnavailableException(Throwable cause) {
    super("Object storage is unavailable", cause);
  }
}
