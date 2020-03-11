package org.aba.athena.iamsupport.plugin;

import com.simba.athena.iamsupport.model.CredentialsHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AdfsMfaCredentialsProviderTest {

    @Test
    void testMyAdfs() {
        assumeFalse(Boolean.valueOf(get("skipIntegration", "true")));
        String host = get("host", "adfs.saml.host");
        String user = get("user", "user@domain");
        String password = get("password", "pwd");

        AdfsMfaCredentialsProvider subject = new AdfsMfaCredentialsProvider();
        String[] params = new String[]{"AwsRegion=eu-west-1",
                "S3OutputLocation=s3://temp/athena/output/",
                "idp_host=" + host,
                "idp_port=443",
                "User=" + user,
                "Password=" + password,
                "preferred_role=arn:aws:iam::1234567890:role/AthenaRole",
                "ssl_insecure=true",
                "LogLevel=6",
                "LogPath=athena_logs",
                "UseAwsLogger=1"
        };
        for (String param : params) {
            String[] pair = param.split("=");
            subject.addParameter(pair[0], pair[1]);
        }

        CredentialsHolder credentials = subject.getCredentials();
        assertNotNull(credentials.getAWSAccessKeyId());
    }

    private String get(String key, String def) {
        return System.getenv().getOrDefault(key, def);
    }
}