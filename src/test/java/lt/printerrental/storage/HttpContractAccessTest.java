package lt.printerrental.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.net.http.HttpResponse;
import java.util.UUID;
import lt.printerrental.auth.CallerToken;
import lt.printerrental.auth.CurrentUser;
import lt.printerrental.transport.ServiceHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class HttpContractAccessTest {
  final ServiceHttpClient client = mock(ServiceHttpClient.class);
  final CallerToken caller = mock(CallerToken.class);
  final CurrentUser currentUser = mock(CurrentUser.class);
  final HttpContractAccess access =
      new HttpContractAccess(client, caller, currentUser, "http://rental.test");
  final UUID id = UUID.randomUUID();

  @Test
  void customerCannotUploadEvenWithoutAnHttpController() {
    assertThatThrownBy(() -> access.requireWrite(id)).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(client, caller);
  }

  @Test
  @SuppressWarnings("unchecked")
  void staffUploadStillVerifiesTheParentContractUsingTheCallerToken() {
    when(currentUser.isStaff()).thenReturn(true);
    when(caller.bearer()).thenReturn("Bearer customer-token");
    var response = (HttpResponse<byte[]>) mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(client.send(
            "http://rental.test",
            "/api/contracts/" + id,
            "GET",
            null,
            null,
            "Bearer customer-token"))
        .thenReturn(response);
    access.requireWrite(id);
    verify(client)
        .send(
            "http://rental.test",
            "/api/contracts/" + id,
            "GET",
            null,
            null,
            "Bearer customer-token");
  }

  @Test
  @SuppressWarnings("unchecked")
  void rejectedParentOwnershipCannotBeBypassed() {
    var response = (HttpResponse<byte[]>) mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(403);
    when(client.send("http://rental.test", "/api/contracts/" + id, "GET", null, null, null))
        .thenReturn(response);
    assertThatThrownBy(() -> access.requireRead(id)).isInstanceOf(AccessDeniedException.class);
  }
}
