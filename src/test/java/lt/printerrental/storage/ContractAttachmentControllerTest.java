package lt.printerrental.storage;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lt.printerrental.auth.SecurityConfiguration;
import lt.printerrental.auth.SigningKeyProvider;
import lt.printerrental.auth.UserJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(
    controllers = ContractAttachmentController.class,
    properties = "AWS_ENDPOINT_URL_S3=http://localhost:9000")
@Import({SecurityConfiguration.class, StorageExceptionHandler.class})
class ContractAttachmentControllerTest {
  @MockitoBean JwtDecoder decoder;
  @MockitoBean SigningKeyProvider signingKeys;
  @MockitoBean UserJwtAuthenticationConverter converter;
  @MockitoBean ContractAttachmentService service;
  @Autowired MockMvc mvc;

  private final UUID contractId = UUID.randomUUID();

  @Test
  void returns400OnInvalidUpload() throws Exception {
    var empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
    mvc.perform(
            multipart("/api/contracts/{contractId}/attachments", contractId)
                .file(empty)
                .with(user("staff").roles("EMPLOYEE")))
        .andExpect(status().is(400))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Attachment must contain 1 to 5242880 bytes"));

    var dangerous =
        new MockMultipartFile(
            "file", "payload.html", "text/html", "<script>alert(1)</script>".getBytes());
    mvc.perform(
            multipart("/api/contracts/{contractId}/attachments", contractId)
                .file(dangerous)
                .with(user("staff").roles("EMPLOYEE")))
        .andExpect(status().is(400))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Attachment file type is not allowed"));

    verifyNoInteractions(service);
  }

  @Test
  void returns413OnMaxUploadSizeExceeded() throws Exception {
    when(service.upload(eq(contractId), any(AttachmentUpload.class)))
        .thenThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024));
    var file = new MockMultipartFile("file", "report.pdf", "application/pdf", "pdf".getBytes());
    mvc.perform(
            multipart("/api/contracts/{contractId}/attachments", contractId)
                .file(file)
                .with(user("staff").roles("EMPLOYEE")))
        .andExpect(status().is(413))
        .andExpect(jsonPath("$.status").value(413))
        .andExpect(jsonPath("$.detail").value("Attachment exceeds maximum allowed size"));
  }

  @Test
  void returnsSafeDownloadHeaders() throws Exception {
    var attachmentId = UUID.randomUUID();
    var bytes = "pdf-bytes".getBytes(StandardCharsets.UTF_8);
    var metadata =
        new ContractAttachment(
            attachmentId,
            contractId,
            "contract.pdf",
            "application/octet-stream",
            bytes.length,
            Instant.EPOCH);
    when(service.download(contractId, attachmentId))
        .thenReturn(new ContractAttachmentService.Download(metadata, bytes));

    mvc.perform(
            get("/api/contracts/{contractId}/attachments/{id}", contractId, attachmentId)
                .with(user("alice").roles("CUSTOMER")))
        .andExpect(status().is(200))
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    allOf(startsWith("attachment;"), containsString("contract.pdf"))))
        .andExpect(content().bytes(bytes));
  }

  @Test
  void uploadResponseIncludesObjectKey() throws Exception {
    var attachmentId = UUID.randomUUID();
    var bytes = "pdf-bytes".getBytes(StandardCharsets.UTF_8);
    var attachment =
        new ContractAttachment(
            attachmentId,
            contractId,
            "contract.pdf",
            "application/octet-stream",
            bytes.length,
            Instant.EPOCH);
    when(service.upload(eq(contractId), any(AttachmentUpload.class))).thenReturn(attachment);

    var file = new MockMultipartFile("file", "contract.pdf", "application/pdf", bytes);
    mvc.perform(
            multipart("/api/contracts/{contractId}/attachments", contractId)
                .file(file)
                .with(user("staff").roles("EMPLOYEE")))
        .andExpect(status().is(201))
        .andExpect(jsonPath("$.objectKey").value(attachment.getObjectKey()))
        .andExpect(jsonPath("$.filename").value("contract.pdf"));
  }

  @Test
  void rejectsPageAboveMaximumBound() throws Exception {
    mvc.perform(
            get("/api/contracts/{contractId}/attachments", contractId)
                .param("page", "10001")
                .with(user("alice").roles("CUSTOMER")))
        .andExpect(status().is(400));
    verifyNoInteractions(service);
  }
}
