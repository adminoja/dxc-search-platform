package th.go.dxc.platform.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import th.go.dxc.platform.search.application.audit.service.PiiRedactor;

@Configuration
public class AuditConfig {
    @Bean
    PiiRedactor piiRedactor(
            @Value("${platform.audit.redactor.key-id:v1}") String keyId,
            @Value("${platform.audit.redactor.hmac-key-b64}") String keyB64,
            @Value("${platform.audit.redactor.output:hex}") String output) {
        return new PiiRedactor(keyId, keyB64, output);
    }

}
