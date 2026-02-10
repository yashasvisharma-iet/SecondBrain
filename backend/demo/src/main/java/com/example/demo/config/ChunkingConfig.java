package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChunkingConfig {

    @Value("${chunking.max-chars:400}")
    private int maxChars;

    @Value("${chunking.min-chars:100}")
    private int minChars;

    public int getMaxChars() {
        return maxChars;
    }

    public int getMinChars() {
        return minChars;
    }
}
