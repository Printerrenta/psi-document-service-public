package lt.printerrental.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.printerrental.transport.ServiceHttpClient;
import lt.printerrental.transport.UpstreamUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.Jwt;

class HttpUserIdentityResolverTest {
  @Test
  void wrapsUpstreamUnavailableExceptionInAuthenticationServiceException() {
    var client = mock(ServiceHttpClient.class);
    var resolver =
        new HttpUserIdentityResolver("http://identity.test", client, new ObjectMapper());
    var jwt =
        Jwt.withTokenValue("signed-token")
            .header("alg", "EdDSA")
            .subject("user-1")
            .build();
    when(client.send(
            "http://identity.test", "/api/auth/me", "GET", null, null, "Bearer signed-token"))
        .thenThrow(new UpstreamUnavailableException());

    assertThatThrownBy(() -> resolver.resolve(jwt))
        .isInstanceOf(AuthenticationServiceException.class)
        .hasMessage("Identity service unavailable")
        .satisfies(
            error ->
                assertThat(error.getCause()).isInstanceOf(UpstreamUnavailableException.class));
  }
}
