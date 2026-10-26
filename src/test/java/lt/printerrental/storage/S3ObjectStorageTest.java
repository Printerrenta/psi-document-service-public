package lt.printerrental.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class S3ObjectStorageTest {
  @Test
  void missingObjectIsReportedWithoutLeakingTheSdkException() {
    var client = mock(S3Client.class);
    when(client.getObjectAsBytes(
            org.mockito.ArgumentMatchers.<Consumer<GetObjectRequest.Builder>>any()))
        .thenThrow(NoSuchKeyException.builder().message("internal provider detail").build());
    assertThatThrownBy(() -> new S3ObjectStorage(client, "assets").download("missing"))
        .isInstanceOf(AttachmentNotFoundException.class)
        .hasMessage("Attachment not found");
  }

  @Test
  void providerFailureIsMappedToTheStoragePortFailure() {
    var client = mock(S3Client.class);
    when(client.getObjectAsBytes(
            org.mockito.ArgumentMatchers.<Consumer<GetObjectRequest.Builder>>any()))
        .thenThrow(SdkClientException.create("internal provider detail"));
    assertThatThrownBy(() -> new S3ObjectStorage(client, "assets").download("file"))
        .isInstanceOf(StorageUnavailableException.class)
        .hasMessage("Object storage is unavailable");
  }
}
