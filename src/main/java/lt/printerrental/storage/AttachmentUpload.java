package lt.printerrental.storage;

import java.util.Locale;
import java.util.Set;

public record AttachmentUpload(String filename, byte[] bytes) {
  private static final Set<String> BLOCKED_EXTENSIONS =
      Set.of(
          ".exe", ".bat", ".cmd", ".com", ".msi", ".scr", ".pif", ".sh", ".ps1", ".vbs", ".js",
          ".mjs", ".html", ".htm", ".xhtml", ".svg");

  public AttachmentUpload {
    if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
      throw new IllegalArgumentException("Attachment must contain 1 to 5242880 bytes");
    filename = filename == null ? "attachment" : filename;
    filename = filename.replace('\\', '/');
    filename =
        filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_").strip();
    if (filename.isBlank() || filename.length() > 200)
      throw new IllegalArgumentException("Attachment filename must contain 1 to 200 characters");
    if (filename.equals(".") || filename.equals(".."))
      throw new IllegalArgumentException("Attachment filename is not allowed");
    var lower = filename.toLowerCase(Locale.ROOT);
    int dot = lower.lastIndexOf('.');
    if (dot >= 0 && BLOCKED_EXTENSIONS.contains(lower.substring(dot)))
      throw new IllegalArgumentException("Attachment file type is not allowed");
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
