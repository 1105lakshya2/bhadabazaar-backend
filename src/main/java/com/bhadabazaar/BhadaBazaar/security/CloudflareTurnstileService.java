package com.bhadabazaar.BhadaBazaar.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CloudflareTurnstileService {

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    @Value("${cloudflare.turnstile.url}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String token, String remoteIp) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", secretKey);
        map.add("response", token);
        if (remoteIp != null) {
            map.add("remoteip", remoteIp);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("success"));
        } catch (Exception e) {
            return false;
        }
    }
}
