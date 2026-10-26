package lt.printerrental.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class StorageConfiguration {
  @Bean(destroyMethod = "close")
  S3Client s3Client(Environment environment) {
    return S3Client.builder()
        .endpointOverride(URI.create(environment.getRequiredProperty("AWS_ENDPOINT_URL_S3")))
        .region(Region.of(environment.getRequiredProperty("AWS_REGION")))
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .forcePathStyle(true)
        .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(30)))
        .httpClientBuilder(
            UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(10))
                .socketTimeout(Duration.ofSeconds(20)))
        .build();
  }
}
