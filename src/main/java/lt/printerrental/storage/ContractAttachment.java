package lt.printerrental.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "contract_attachments")
public class ContractAttachment {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID contractId;

  @Column(nullable = false, unique = true, length = 200)
  private String objectKey;

  @Column(nullable = false, length = 200)
  private String filename;

  @Column(nullable = false, length = 120)
  private String contentType;

  @Column(nullable = false)
  private long byteSize;

  @Column(nullable = false)
  private Instant createdAt;

  protected ContractAttachment() {}

  ContractAttachment(
      UUID id, UUID contractId, String filename, String contentType, long byteSize, Instant now) {
    this.id = id;
    this.contractId = contractId;
    this.objectKey = "contracts/" + contractId + "/" + id;
    this.filename = filename;
    this.contentType = contentType;
    this.byteSize = byteSize;
    this.createdAt = now.truncatedTo(ChronoUnit.MICROS);
  }

  public UUID getId() {
    return id;
  }

  public UUID getContractId() {
    return contractId;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getFilename() {
    return filename;
  }

  public String getContentType() {
    return contentType;
  }

  public long getByteSize() {
    return byteSize;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
