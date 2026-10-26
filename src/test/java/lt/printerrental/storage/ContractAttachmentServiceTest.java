package lt.printerrental.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ContractAttachmentServiceTest {
  @Mock ObjectStorage storage;
  @Mock ContractAccess access;
  @Mock ContractAttachmentRepository attachments;
  private ContractAttachmentService service;
  private final UUID contractId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    service =
        new ContractAttachmentService(
            storage, access, attachments, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
  }

  @Test
  void rejectsEmptyFilesBeforeStorageUpload() {
    var file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
    assertThatThrownBy(
            () ->
                service.upload(
                    contractId, new AttachmentUpload(file.getOriginalFilename(), file.getBytes())))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(storage, attachments);
  }

  @Test
  @SuppressWarnings("unchecked")
  void stripsUserSuppliedPathAndDoesNotTrustHtmlContentType() throws Exception {
    when(attachments.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var file =
        new MockMultipartFile(
            "file", "../../private\\document.html", "text/html", "hello".getBytes());
    var result =
        service.upload(
            contractId, new AttachmentUpload(file.getOriginalFilename(), file.getBytes()));
    assertThat(result.getFilename()).isEqualTo("document.html");
    assertThat(result.getContentType()).isEqualTo("application/octet-stream");
    assertThat(result.getObjectKey()).startsWith("contracts/" + contractId + "/");
    verify(storage)
        .upload(
            org.mockito.ArgumentMatchers.eq(result.getObjectKey()),
            org.mockito.ArgumentMatchers.eq("application/octet-stream"),
            org.mockito.ArgumentMatchers.eq("hello".getBytes()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void removesUploadedObjectWhenMetadataCannotBeCommitted() {
    when(attachments.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("simulated DB failure"));
    var file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
    assertThatThrownBy(
            () ->
                service.upload(
                    contractId, new AttachmentUpload(file.getOriginalFilename(), file.getBytes())))
        .isInstanceOf(DataIntegrityViolationException.class);
    verify(storage).delete(any(String.class));
  }

  @Test
  void attachmentFromAnotherContractIsNotDownloaded() {
    var id = UUID.randomUUID();
    when(attachments.findByIdAndContractId(id, contractId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.download(contractId, id))
        .isInstanceOf(AttachmentNotFoundException.class);
    verifyNoInteractions(storage);
  }

  @Test
  void deniedReadNeverTouchesStorageOrMetadata() {
    org.mockito.Mockito.doThrow(
            new org.springframework.security.access.AccessDeniedException("denied"))
        .when(access)
        .requireRead(contractId);
    assertThatThrownBy(() -> service.list(contractId, 0, 20))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    assertThatThrownBy(() -> service.download(contractId, UUID.randomUUID()))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    verifyNoInteractions(storage, attachments);
  }

  @Test
  void deniedUploadNeverWritesAFileOrMetadata() {
    org.mockito.Mockito.doThrow(
            new org.springframework.security.access.AccessDeniedException("denied"))
        .when(access)
        .requireWrite(contractId);
    var file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
    assertThatThrownBy(
            () ->
                service.upload(
                    contractId, new AttachmentUpload(file.getOriginalFilename(), file.getBytes())))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    verifyNoInteractions(storage, attachments);
  }

  @Test
  void cleanupFailureDoesNotHideTheOriginalDatabaseFailure() {
    when(attachments.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("original"));
    org.mockito.Mockito.doThrow(new StorageUnavailableException(new RuntimeException("cleanup")))
        .when(storage)
        .delete(any(String.class));
    var file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
    assertThatThrownBy(
            () ->
                service.upload(
                    contractId, new AttachmentUpload(file.getOriginalFilename(), file.getBytes())))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("original");
  }
}
