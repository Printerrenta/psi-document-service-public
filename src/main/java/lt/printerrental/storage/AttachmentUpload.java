package lt.printerrental.storage;

public record AttachmentUpload(String filename, byte[] bytes) {
  public AttachmentUpload {
    if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
      throw new IllegalArgumentException("Attachment must contain 1 to 5242880 bytes");
    filename = filename == null ? "attachment" : filename;
    filename = filename.replace('\\', '/');
    filename =
        filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_").strip();
    if (filename.isBlank() || filename.length() > 200)
      throw new IllegalArgumentException("Attachment filename must contain 1 to 200 characters");
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }

  public int size() {
    return bytes.length;
  }
}
