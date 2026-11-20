package lt.printerrental.auth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtException;

public final class RemoteJwksSigningKeyProvider implements SigningKeyProvider {
  private static final Logger log = LoggerFactory.getLogger(RemoteJwksSigningKeyProvider.class);
  private final URI uri;
  private final HttpClient http;
  private final Clock clock;
  private JWKSet keys;
  private Instant refreshed = Instant.EPOCH;
  private Instant retryAfter = Instant.EPOCH;

  public RemoteJwksSigningKeyProvider(URI uri, HttpClient http, Clock clock) {
    this.uri = uri;
    this.http = http;
    this.clock = clock;
  }

  @Override
  public synchronized JWK findById(String kid) {
    var now = clock.instant();
    boolean needsRefresh =
        keys == null
            || refreshed.plusSeconds(300).isBefore(now)
            || (keys.getKeyByKeyId(kid) == null && refreshed.plusSeconds(10).isBefore(now));
    if (needsRefresh && !now.isBefore(retryAfter)) {
      refresh(now);
    }
    return keys.getKeyByKeyId(kid);
  }

  private void refresh(Instant now) {
    try {
      var response =
          http.send(
              HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200 || response.body().length() > 65536) {
        throw new JwtException("JWKS unavailable");
      }
      keys = JWKSet.parse(response.body());
      refreshed = now;
      retryAfter = Instant.EPOCH;
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      handleRefreshFailure(now, new JwtException("Signing keys unavailable", error));
    } catch (JwtException error) {
      handleRefreshFailure(now, error);
    } catch (Exception error) {
      handleRefreshFailure(now, new JwtException("Signing keys unavailable", error));
    }
  }

  private void handleRefreshFailure(Instant now, JwtException error) {
    if (keys != null) {
      log.warn("Failed to refresh JWKS from {}; falling back to cached signing keys", uri, error);
      retryAfter = now.plusSeconds(10);
      return;
    }
    log.warn("Failed to load JWKS from {} with no cached signing keys available", uri, error);
    throw error;
  }
}
