package lt.printerrental.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Component
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class S3ObjectStorage implements ObjectStorage {
  private final S3Client client;
  private final String bucket;

  public S3ObjectStorage(S3Client client, @Value("${app.storage.bucket}") String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  @Override
  public void upload(String key, String contentType, byte[] bytes) {
    try {
      client.putObject(
          input -> input.bucket(bucket).key(key).contentType(contentType),
          RequestBody.fromBytes(bytes));
    } catch (SdkException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public byte[] download(String key) {
    try {
      return client.getObjectAsBytes(input -> input.bucket(bucket).key(key)).asByteArray();
    } catch (NoSuchKeyException error) {
      throw new AttachmentNotFoundException();
    } catch (SdkException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public void delete(String key) {
    try {
      client.deleteObject(input -> input.bucket(bucket).key(key));
    } catch (SdkException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
