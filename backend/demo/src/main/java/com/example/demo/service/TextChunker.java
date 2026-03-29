package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class TextChunker {

    private static final int MAX_CHARS = 1000;
    private static final int MIN_CHARS = 50;

    public List<String> chunk(String rawText) {
        if (isEmpty(rawText)) return List.of();

        String normalized = normalizeText(rawText);
        List<String> paragraphs = extractParagraphs(normalized);

        return buildChunks(paragraphs);
    }

    // -------------------- CORE --------------------

    private List<String> buildChunks(List<String> paragraphs) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            processParagraph(chunks, current, paragraph);
        }

        flushChunk(chunks, current);
        return chunks;
    }

    private void processParagraph(List<String> chunks, StringBuilder current, String paragraph) {
        if (isTooLarge(paragraph)) {
            processLargeParagraph(chunks, current, paragraph);
            return;
        }

        appendParagraph(current, paragraph);
        flushIfOverflow(chunks, current);
    }

    private void processLargeParagraph(List<String> chunks, StringBuilder current, String paragraph) {
        List<String> sentences = extractSentences(paragraph);

        for (String sentence : sentences) {
            appendSentence(current, sentence);
            flushIfOverflow(chunks, current);
        }
    }

    // -------------------- APPEND --------------------

    private void appendParagraph(StringBuilder current, String paragraph) {
        current.append(paragraph).append("\n\n");
    }

    private void appendSentence(StringBuilder current, String sentence) {
        current.append(sentence).append(" ");
    }

    // -------------------- FLUSH --------------------

    private void flushIfOverflow(List<String> chunks, StringBuilder current) {
        if (isOverflow(current)) {
            flushChunk(chunks, current);
        }
    }

    private void flushChunk(List<String> chunks, StringBuilder current) {
        if (isValidChunk(current)) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }

    // -------------------- VALIDATION --------------------

    private boolean isEmpty(String text) {
        return text == null || text.isBlank();
    }

    private boolean isTooLarge(String text) {
        return text.length() > MAX_CHARS;
    }

    private boolean isOverflow(StringBuilder current) {
        return current.length() > MAX_CHARS;
    }

    private boolean isValidChunk(StringBuilder current) {
        return current.length() >= MIN_CHARS;
    }

    // -------------------- TEXT PROCESSING --------------------

    private String normalizeText(String text) {
        return text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private List<String> extractParagraphs(String text) {
        return Arrays.stream(text.split("\\n{2,}"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<String> extractSentences(String text) {
        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}