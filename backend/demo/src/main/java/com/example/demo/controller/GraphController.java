package com.example.demo.controller;

import com.example.demo.dto.GraphDataDto;
import com.example.demo.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
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
}
