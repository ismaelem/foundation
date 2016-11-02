package io.carvill.foundation.email.sparkpost;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import io.carvill.foundation.email.Email;
import io.carvill.foundation.email.EmailSender;
import io.carvill.foundation.email.Recipient;
import io.carvill.foundation.email.SentResult;
import io.carvill.foundation.email.mandrill.MandrillFake;

/**
 * @author Carlos Carpio, carlos.carpio07@gmail.com
 */
public class Sparkpost extends EmailSender {

    private final static Logger log = LoggerFactory.getLogger(MandrillFake.class);

    public static final String TEMPLATE_API_URL = "https://api.sparkpost.com/api/v1/transmissions";

    private RestTemplate restTemplate;

    private String apiKey;

    private String authorization;

    public Sparkpost(final String apiKey) throws UnsupportedEncodingException {
        this(apiKey, new RestTemplate());
    }

    public Sparkpost(final String apiKey, final RestTemplate restTemplate) throws UnsupportedEncodingException {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;

        final String auth = this.apiKey + ":";
        final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes("UTF-8"));
        this.authorization = ("Basic " + new String(encodedAuth));
    }

    @Override
    public <T extends Recipient> void send(final Email<T> email) {
        Assert.isTrue(email instanceof SparkpostEmail);
        final SparkpostTemplateMessage<T> request = new SparkpostTemplateMessage<T>(email.getTemplate(),
                email.getSubject(), email.getFromEmail(), email.getFromName()).withHeaders(email.getHeaders())
                        .withAttachments(email.getAttachments())
                        .withRecipients(email.getRecipients(), email.getVariableProvider());
        this.send(request, email.getResult());
    }

    protected <T extends Recipient> void send(final SparkpostTemplateMessage<T> request, final SentResult result) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", this.authorization);
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        final HttpEntity<SparkpostTemplateMessage<T>> entity = new HttpEntity<>(request, headers);

        final ResponseEntity<SparkpostResponse> response = this.restTemplate.exchange(TEMPLATE_API_URL, HttpMethod.POST,
                entity, SparkpostResponse.class);
        final SparkpostResponse body = response.getBody();
        if (CollectionUtils.isEmpty(body.getErrors())) {
            final SparkpostResults content = body.getResults();
            if (content == null) {
                result.setFailed(request.getRecipients().size());
            } else {
                this.logErrors(content.getErrors());
                result.setSuccess(content.getAccepted());
                result.setFailed(content.getRejected());
            }
        } else {
            result.setFailed(request.getRecipients().size());
            this.logErrors(body.getErrors());
        }
    }

    protected void logErrors(final List<SparkpostError> errors) {
        if (CollectionUtils.isNotEmpty(errors)) {
            for (SparkpostError error : errors) {
                log.warn("Email error -> {}", error);
            }
        }
    }

    @Override
    public <T extends Recipient> Email<T> buildEmail(final String fromName, final String fromEmail,
            final String template, final String subject) {
        return new SparkpostEmail<>(fromName, fromEmail, template, subject);
    }

}