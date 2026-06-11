package com.bhadabazaar.BhadaBazaar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareImageService {

    @Value("${cloudflare.account-id}")
    private String accountId;

    @Value("${cloudflare.api-token}")
    private String apiToken;

    @Value("${cloudflare.image-delivery-url}")
    private String deliveryUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> ALLOWED_EXT =
            Set.of("jpg", "jpeg", "png", "webp", "avif");

    public Map<String, String> uploadFile(MultipartFile file) {

        validateFileIsImage(file);

        try {

            String url = "https://api.cloudflare.com/client/v4/accounts/"
                    + accountId + "/images/v1";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiToken);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            Map result = (Map) response.getBody().get("result");

            String imageId = result.get("id").toString();

            String imageUrl = deliveryUrl + "/" + imageId + "/public";

            return Map.of(
                    "url", imageUrl,
                    "public_id", imageId
            );

        } catch (HttpClientErrorException e) {
            log.error("Cloudflare upload error - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Cloudflare image upload failed: " + e.getStatusCode(), e);
        } catch (IOException e) {
            throw new RuntimeException("Cloudflare upload failed", e);
        }
    }

    public boolean deleteFile(String imageId) {

        String url = "https://api.cloudflare.com/client/v4/accounts/"
                + accountId + "/images/v1/" + imageId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateFileIsImage(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large");
        }

        String name = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        String ext = name.substring(name.lastIndexOf('.') + 1);

        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Unsupported format");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("file should only be image");
        }
    }
}