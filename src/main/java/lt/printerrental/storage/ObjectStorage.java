package lt.printerrental.storage;

/** File storage operations required by the attachment workflow. */
public interface ObjectStorage {
  void upload(String key, String contentType, byte[] bytes);

  byte[] download(String key);

  void delete(String key);
}
