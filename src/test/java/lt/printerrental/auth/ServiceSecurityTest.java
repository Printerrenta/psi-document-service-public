package lt.printerrental.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

/** Verifies the service's own HTTP policy independently of application use-case authorization. */
@WebMvcTest(ServiceSecurityTest.Probe.class)
@Import({SecurityConfiguration.class, ServiceSecurityTest.Probe.class})
class ServiceSecurityTest {
  @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder decoder;
  @MockitoBean SigningKeyProvider signingKeys;
  @MockitoBean UserJwtAuthenticationConverter converter;
  @Autowired MockMvc mvc;

  @Test
  void restrictsRoutesToThisServiceAndItsRoles() throws Exception {
    mvc.perform(get("/api/contracts/123/attachments")).andExpect(status().is(401));
    mvc.perform(get("/api/contracts/123/attachments").with(user("alice").roles("CUSTOMER")))
        .andExpect(status().is(200));
    mvc.perform(get("/api/contracts/123/attachments/456").with(user("alice").roles("CUSTOMER")))
        .andExpect(status().is(200));
    mvc.perform(post("/api/contracts/123/attachments").with(user("alice").roles("CUSTOMER")))
        .andExpect(status().is(403));
    mvc.perform(post("/api/contracts/123/attachments").with(user("alice").roles("EMPLOYEE")))
        .andExpect(status().is(200));
    mvc.perform(get("/api/auth/me").with(user("alice").roles("ADMIN"))).andExpect(status().is(403));
  }

  @RestController
  static class Probe {
    @RequestMapping({
      "/api/contracts/123/attachments",
      "/api/contracts/123/attachments/456",
      "/api/auth/me"
    })
    String respond() {
      return "ok";
    }
  }
}
