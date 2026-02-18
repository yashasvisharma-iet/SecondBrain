package com.example.demo.service;

import com.example.demo.dto.AimlRelationRequest;
import com.example.demo.dto.AimlRelationResponse;
import com.example.demo.dto.AimlEmbeddingRequest;
import com.example.demo.dto.AimlEmbeddingResponse;
import com.example.demo.dto.GraphEdgeDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Component
public class AimlEmbeddingClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AimlEmbeddingClient(@Value("${aiml.base-url:http://localhost:8001}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    public List<GraphEdgeDto> buildSemanticEdges(AimlRelationRequest payload) {
        try {
            RequestEntity<AimlRelationRequest> request = RequestEntity
                    .post(URI.create(baseUrl + "/relations"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);

            ResponseEntity<AimlRelationResponse> response = restTemplate.exchange(
                    request,
                    AimlRelationResponse.class
            );

            AimlRelationResponse body = response.getBody();
            return body == null || body.edges() == null ? List.of() : body.edges();
        } catch (RestClientException ex) {
            System.err.println("AIML relation service unavailable, continuing without semantic edges: " + ex.getMessage());
            return List.of();
        }
    }

    public List<List<Double>> buildEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            RequestEntity<AimlEmbeddingRequest> request = RequestEntity
                    .post(URI.create(baseUrl + "/embeddings"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AimlEmbeddingRequest(texts));

            ResponseEntity<AimlEmbeddingResponse> response = restTemplate.exchange(
                    request,
                    AimlEmbeddingResponse.class
            );

            AimlEmbeddingResponse body = response.getBody();
            return body == null || body.embeddings() == null ? List.of() : body.embeddings();
        } catch (RestClientException ex) {
            System.err.println("AIML embedding service unavailable, skipping vector persistence: " + ex.getMessage());
            return List.of();
        }
    }
}
