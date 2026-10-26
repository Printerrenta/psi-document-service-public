package lt.printerrental.storage;

public class AttachmentNotFoundException extends RuntimeException {
  public AttachmentNotFoundException() {
    super("Attachment not found");
  }
}
