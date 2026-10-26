package lt.printerrental.storage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contracts/{contractId}/attachments")
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class ContractAttachmentController {
  private final ContractAttachmentService service;

  public ContractAttachmentController(ContractAttachmentService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContractAttachment> upload(
      @PathVariable UUID contractId, @RequestParam("file") MultipartFile file) throws IOException {
    var result =
        service.upload(
            contractId, new AttachmentUpload(file.getOriginalFilename(), file.getBytes()));
    return ResponseEntity.created(
            URI.create("/api/contracts/" + contractId + "/attachments/" + result.getId()))
        .body(result);
  }

  @GetMapping
  public AttachmentPage list(
      @PathVariable UUID contractId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    var result = service.list(contractId, page, size);
    return new AttachmentPage(
        result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
  }

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> download(@PathVariable UUID contractId, @PathVariable UUID id) {
    var download = service.download(contractId, id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(download.metadata().getFilename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(download.bytes());
  }

  public record AttachmentPage(
      List<ContractAttachment> content, int page, int size, long totalElements, int totalPages) {}
}
