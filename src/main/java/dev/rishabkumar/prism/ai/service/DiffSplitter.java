package dev.rishabkumar.prism.ai.service;

import java.util.ArrayList;
import java.util.List;

public final class DiffSplitter {

    private DiffSplitter() {}

    private static final String FILE_BOUNDARY = "diff --git ";

    public static List<String> splitByFile(String diff) {
        List<String> files = new ArrayList<>();
        int start = diff.indexOf(FILE_BOUNDARY);

        if (start < 0) {
            files.add(diff);
            return files;
        }

        while (start >= 0) {
            int next = diff.indexOf(FILE_BOUNDARY, start + FILE_BOUNDARY.length());
            if (next < 0) {
                files.add(diff.substring(start));
                break;
            }
            files.add(diff.substring(start, next));
            start = next;
        }

        return files;
    }

    public static List<String> groupIntoChunks(List<String> fileDiffs, int maxCharsPerChunk) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String fileDiff : fileDiffs) {
            String entry = fileDiff.length() > maxCharsPerChunk
                    ? fileDiff.substring(0, maxCharsPerChunk) + "\n[File diff truncated — too large]"
                    : fileDiff;

            if (current.length() > 0 && current.length() + entry.length() > maxCharsPerChunk) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }

            current.append(entry);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }

        return chunks;
    }
}
