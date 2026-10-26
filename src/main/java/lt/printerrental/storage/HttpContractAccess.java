package lt.printerrental.storage;

import java.util.UUID;
import lt.printerrental.auth.*;
import lt.printerrental.transport.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpContractAccess implements ContractAccess {
  private final ServiceHttpClient client;
  private final CallerToken caller;
  private final CurrentUser currentUser;
  private final String url;

  public HttpContractAccess(
      ServiceHttpClient client,
      CallerToken caller,
      CurrentUser currentUser,
      @Value("${RENTAL_SERVICE_URL}") String url) {
    this.client = client;
    this.caller = caller;
    this.currentUser = currentUser;
    this.url = url;
  }

  public void requireRead(UUID id) {
    var response = client.send(url, "/api/contracts/" + id, "GET", null, null, caller.bearer());
    if (response.statusCode() == 403)
      throw new org.springframework.security.access.AccessDeniedException("Access denied");
    if (response.statusCode() == 404)
      throw new ResponseStatusException(HttpStatusCode.valueOf(404), "Contract not found");
    if (response.statusCode() != 200) throw new UpstreamUnavailableException();
  }

  public void requireWrite(UUID id) {
    if (!currentUser.isStaff())
      throw new org.springframework.security.access.AccessDeniedException("Staff access required");
    requireRead(id);
  }
}
