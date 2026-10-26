package lt.printerrental.storage;

import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class ContractAttachmentService {
  private final ObjectStorage storage;
  private final ContractAccess access;
  private final ContractAttachmentRepository attachments;
  private final Clock clock;

  public ContractAttachmentService(
      ObjectStorage storage,
      ContractAccess access,
      ContractAttachmentRepository attachments,
      Clock clock) {
    this.storage = storage;
    this.access = access;
    this.attachments = attachments;
    this.clock = clock;
  }

  public ContractAttachment upload(UUID contractId, AttachmentUpload file) {
    access.requireWrite(contractId);
    // Serve downloads as attachments with a safe generic type; do not trust client MIME headers.
    var attachment =
        new ContractAttachment(
            UUID.randomUUID(),
            contractId,
            file.filename(),
            "application/octet-stream",
            file.size(),
            clock.instant());
    storage.upload(attachment.getObjectKey(), attachment.getContentType(), file.bytes());
    // Repository saveAndFlush commits before returning; cleanup also catches commit failures.
    try {
      return attachments.saveAndFlush(attachment);
    } catch (RuntimeException error) {
      try {
        storage.delete(attachment.getObjectKey());
      } catch (StorageUnavailableException cleanupError) {
        // Keep the original DB failure. The key can be recovered from server-side storage logs.
      }
      throw error;
    }
  }

  public Page<ContractAttachment> list(UUID contractId, int page, int size) {
    access.requireRead(contractId);
    return attachments.findByContractId(
        contractId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
  }

  public Download download(UUID contractId, UUID id) {
    access.requireRead(contractId);
    var attachment =
        attachments
            .findByIdAndContractId(id, contractId)
            .orElseThrow(AttachmentNotFoundException::new);
    return new Download(attachment, storage.download(attachment.getObjectKey()));
  }

  public record Download(ContractAttachment metadata, byte[] bytes) {}
}
