package lt.printerrental.transport;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceHttpClient {
  private static final Logger log = LoggerFactory.getLogger(ServiceHttpClient.class);
  private final ServiceCredentials credentials;
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public ServiceHttpClient(ServiceCredentials credentials) {
    this.credentials = credentials;
  }

  public HttpResponse<byte[]> send(
      String serviceUrl,
      String path,
      String method,
      byte[] body,
      String contentType,
      String bearer) {
    var baseUrl = stripTrailingSlashes(serviceUrl);
    var safePath = stripQuery(path);
    try {
      var request =
          HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30));
      if (contentType != null) request.header("Content-Type", contentType);
      if (bearer != null) request.header("Authorization", bearer);
      var cloud = credentials.identityToken(baseUrl);
      if (cloud != null) request.header("X-Serverless-Authorization", "Bearer " + cloud);
      request.method(
          method,
          body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofByteArray(body));
      var response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 500) {
        log.warn(
            "Upstream HTTP {} {}{} returned status {}",
            method,
            baseUrl,
            safePath,
            response.statusCode());
      }
      return response;
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      log.warn("Upstream HTTP {} {}{} interrupted", method, baseUrl, safePath, error);
      throw new UpstreamUnavailableException();
    } catch (Exception error) {
      log.warn("Upstream HTTP {} {}{} failed", method, baseUrl, safePath, error);
      throw new UpstreamUnavailableException();
    }
  }

  private static String stripTrailingSlashes(String url) {
    if (url == null) return "";
    int end = url.length();
    while (end > 0 && url.charAt(end - 1) == '/') {
      end--;
    }
    return url.substring(0, end);
  }

  private static String stripQuery(String path) {
    if (path == null) return "";
    int query = path.indexOf('?');
    return query >= 0 ? path.substring(0, query) : path;
  }
}
