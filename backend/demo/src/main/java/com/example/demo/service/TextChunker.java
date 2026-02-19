package com.example.demo.service;


import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class TextChunker {

    private final int MAX_CHARS;
    private final int MIN_CHARS;
    public TextChunker() {
        this.MAX_CHARS = 1000;
        this.MIN_CHARS = 200;
    }


    public List<String> chunk(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        String normalized = normalize(rawText);
        List<String> paragraphs = splitParagraphs(normalized);

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() > MAX_CHARS) {
                flushIfValid(chunks, current);
            }

            if (para.length() > MAX_CHARS) {
                List<String> sentences = splitSentences(para);
                for (String sentence : sentences) {
                    if (current.length() + sentence.length() > MAX_CHARS) {
                        flushIfValid(chunks, current);
                    }
                    current.append(sentence).append(" ");
                }
            } else {
                current.append(para).append("\n\n");
            }
        }

        flushIfValid(chunks, current);

        return chunks;
    }

    // ---------------- helpers ----------------

    private String normalize(String text) {
        return text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private List<String> splitParagraphs(String text) {
        return Arrays.stream(text.split("\\n{2,}"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<String> splitSentences(String text) {
        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private void flushIfValid(List<String> chunks, StringBuilder current) {
        if (current.length() >= MIN_CHARS) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }
}
