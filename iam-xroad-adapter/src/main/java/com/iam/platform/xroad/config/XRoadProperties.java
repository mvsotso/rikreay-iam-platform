package com.iam.platform.xroad.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "xroad")
public class XRoadProperties {

    private boolean enabled = true;
    private Member member = new Member();
    private Acl acl = new Acl();
    private Routing routing = new Routing();

    @Data
    public static class Member {
        private String instance = "KH";
        private String memberClass = "GOV";
        private String memberCode = "GDT";
        private String subsystem = "TAX-SERVICES";

        public String getFullIdentifier() {
            return instance + "/" + memberClass + "/" + memberCode + "/" + subsystem;
        }
    }

    @Data
    public static class Acl {
        private int cacheTtlMinutes = 5;
    }

    @Data
    public static class Routing {
        private String coreServiceUrl = "http://localhost:8082";
    }
}
