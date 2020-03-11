package org.aba.athena.iamsupport.plugin;

import com.simba.athena.amazonaws.SdkClientException;
import com.simba.athena.amazonaws.util.IOUtils;
import com.simba.athena.amazonaws.util.StringUtils;
import com.simba.athena.iamsupport.plugin.AdfsCredentialsProvider;
import com.simba.athena.shaded.apache.http.client.entity.UrlEncodedFormEntity;
import com.simba.athena.shaded.apache.http.client.methods.CloseableHttpResponse;
import com.simba.athena.shaded.apache.http.client.methods.HttpGet;
import com.simba.athena.shaded.apache.http.client.methods.HttpPost;
import com.simba.athena.shaded.apache.http.client.methods.HttpRequestBase;
import com.simba.athena.shaded.apache.http.impl.client.CloseableHttpClient;
import com.simba.athena.shaded.apache.http.message.BasicNameValuePair;
import com.simba.athena.shaded.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.simba.athena.amazonaws.util.StringUtils.isNullOrEmpty;

public class AdfsMfaCredentialsProvider extends AdfsCredentialsProvider {
    private static final Pattern SAML_PATTERN = Pattern.compile("SAMLResponse\\W+value=\"([^\"]+)\"");

    private static String execute(CloseableHttpClient httpClient, HttpRequestBase method) throws IOException {
        String responseContent;
        CloseableHttpResponse response = httpClient.execute(method);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Failed send request: " + response.getStatusLine().getReasonPhrase());
        }
        responseContent = EntityUtils.toString(response.getEntity());
        return responseContent;
    }

    protected String getSamlAssertion() throws IOException {
        if (isNullOrEmpty(this.m_idpHost)) {
            throw new IOException("Missing required property: idp_host");
        } else {
            if (isNullOrEmpty(this.m_userName) || StringUtils.isNullOrEmpty(this.m_password)) {
                throw new RuntimeException("Please provide credentials");
            }
            return this.formBasedAuthenticationWithMFA();
        }
    }

    private String formBasedAuthenticationWithMFA() throws IOException {
        String idpUrl = "https://" + this.m_idpHost + ':' + this.m_idpPort + "/adfs/ls/IdpInitiatedSignOn.aspx?loginToRp=urn:amazon:webservices";
        CloseableHttpClient httpClient = null;

        try {
            httpClient = this.getHttpClient();
            HttpGet getLoginForm = new HttpGet(idpUrl);
            String responseContent = execute(httpClient, getLoginForm);

            //prepare login form vars
            ArrayList<BasicNameValuePair> postVars = collectFormVars(responseContent);
            idpUrl = getFormActionUrl(idpUrl, responseContent);

            //post login form
            HttpPost postCredentials = newPost(idpUrl, postVars);
            responseContent = execute(httpClient, postCredentials);

            //prepare MFA form vars
            postVars = collectFormVars(responseContent);
            idpUrl = getFormActionUrl(idpUrl, responseContent);

            //post MFA form
            HttpPost postMfa = newPost(idpUrl, postVars);
            responseContent = execute(httpClient, postMfa);

            Matcher matcher = SAML_PATTERN.matcher(responseContent);
            if (!matcher.find()) {
                throw new IOException("Failed to login ADFS.");
            }

            return matcher.group(1);
        } catch (GeneralSecurityException ex) {
            throw new SdkClientException("Failed create SSLContext.", ex);
        } finally {
            IOUtils.closeQuietly(httpClient, null);
        }
    }

    private HttpPost newPost(String idpUrl, ArrayList<BasicNameValuePair> postVars) throws UnsupportedEncodingException {
        HttpPost postCredentials = new HttpPost(idpUrl);
        postCredentials.setEntity(new UrlEncodedFormEntity(postVars));
        return postCredentials;
    }

    private String getFormActionUrl(String idpUrl, String responseContent) {
        String formActionUrl = this.getFormAction(responseContent);
        if (!isNullOrEmpty(formActionUrl) && formActionUrl.startsWith("/")) {
            idpUrl = "https://" + this.m_idpHost + ':' + this.m_idpPort + formActionUrl;
        }
        return idpUrl;
    }

    private ArrayList<BasicNameValuePair> collectFormVars(String responseContent) {
        String lowerCaseInputName;
        ArrayList<BasicNameValuePair> postVars = new ArrayList<>(4);
        Iterator inputTags = this.getInputTagsfromHTML(responseContent).iterator();

        String inputName;
        while (inputTags.hasNext()) {
            String inputTag = (String) inputTags.next();
            inputName = this.getValueByKey(inputTag, "name");
            String inputValue = this.getValueByKey(inputTag, "value");
            lowerCaseInputName = inputName.toLowerCase();
            if (lowerCaseInputName.contains("username")) {
                postVars.add(new BasicNameValuePair(inputName, this.m_userName));
            } else if (lowerCaseInputName.contains("authmethod")) {
                if (!inputValue.isEmpty()) {
                    postVars.add(new BasicNameValuePair(inputName, inputValue));
                }
            } else if (lowerCaseInputName.contains("password")) {
                postVars.add(new BasicNameValuePair(inputName, this.m_password));
            } else if (!inputName.isEmpty()) {
                postVars.add(new BasicNameValuePair(inputName, inputValue));
            }
        }
        return postVars;
    }
}
