package com.example.demo.controller;

import com.example.demo.dto.AgentQueryRequest;
import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.NoteSummaryRequest;
import com.example.demo.dto.NoteSummaryResponse;
import com.example.demo.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/feed")
    public GraphDataDto feedGraph() {
        return graphService.getFeedGraph();
    }

    @PostMapping("/ask")
    public AgentQueryResponse askAgent(@RequestBody AgentQueryRequest request) {
        return graphService.answerFromDatabase(request.query());
    }

    @PostMapping("/summarize")
    public NoteSummaryResponse summarize(@RequestBody NoteSummaryRequest request) {
        return new NoteSummaryResponse(graphService.summarizePage(request.pageId()));
    }
}
