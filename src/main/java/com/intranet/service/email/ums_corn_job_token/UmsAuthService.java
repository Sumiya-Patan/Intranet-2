package com.intranet.service.email.ums_corn_job_token;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.intranet.util.EmailUtil;

@Service
@RequiredArgsConstructor
public class UmsAuthService {

    @Value("${timesheet.user}")
    private String umsEmail;

    @Value("${timesheet.password}")
    private String umsPassword;

    @Value("${ums.api.base-url}")
    private String umsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private final EmailUtil emailUtil;

    @Data
    private static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String access_token;
        private String token_type;
        private String redirect;
    }

    /**
     * Login to UMS and fetch Bearer Token.
     */
    public String getUmsToken() {
        try {
            LoginRequest request = new LoginRequest();
            request.setEmail(umsEmail);
            request.setPassword(umsPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

            String url = umsBaseUrl + "/auth/login";

            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    LoginResponse.class
            );

            if (response.getBody() == null || response.getBody().getAccess_token() == null) {
                emailUtil.sendEmail(umsEmail, "UMS Login Failed for Weekly Timesheet Remainder", "<h1>UMS login failed: No access token received.</h1>");
                throw new IllegalStateException("UMS login failed: No access token.");
            }
            
            System.out.println("✅ Successfully logged into UMS.");
            return response.getBody().getAccess_token();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to login to UMS: " + ex.getMessage());
        }
    }
}
