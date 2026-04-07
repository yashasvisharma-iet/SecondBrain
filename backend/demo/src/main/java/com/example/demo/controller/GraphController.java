package com.example.demo.controller;

import com.example.demo.dto.AgentQueryRequest;
import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.NoteSummaryRequest;
import com.example.demo.dto.NoteSummaryResponse;
import com.example.demo.dto.RecommendationDto;
import com.example.demo.entity.AppUser;
import com.example.demo.service.UserService;
import com.example.demo.service.graph.GraphService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;
    private final UserService currentUserService;

    public GraphController(GraphService graphService, UserService currentUserService) {
        this.graphService = graphService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/feed")
    public GraphDataDto feedGraph(@AuthenticationPrincipal OAuth2User principal) {
        AppUser user = currentUserService.getOrCreateProfile(principal);
        return graphService.getFeedGraph(user.getId());
    }

    // @PostMapping("/recommendations")
    // public RecommendationDto recommendations(@AuthenticationPrincipal OAuth2User principal) {
    //     AppUser user = currentUserService.getOrCreateProfile(principal);
    //     return graphService.getRecommendations(user.getId());
    // }

    @PostMapping("/ask")
    public AgentQueryResponse askAgent(@AuthenticationPrincipal OAuth2User principal,
                                       @RequestBody AgentQueryRequest request) {
        AppUser user = currentUserService.getOrCreateProfile(principal);
        return graphService.answerFromDatabase(user.getId(), request.query());
    }

    @PostMapping("/summarize")
    public NoteSummaryResponse summarize(@AuthenticationPrincipal OAuth2User principal,
                                         @RequestBody NoteSummaryRequest request) {
        AppUser user = currentUserService.getOrCreateProfile(principal);
        return new NoteSummaryResponse(graphService.summarizePage(user.getId(), request.pageId()));
    }
}
