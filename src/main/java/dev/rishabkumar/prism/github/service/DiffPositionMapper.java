package dev.rishabkumar.prism.github.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts (filePath, fileLineNumber) -> diff position for GitHub's Review API.
 * <p>
 * Diff position = the 1-based index of a line counting every line
 * (context, added, removed) after the @@ header in that file's hunk(s).
 */
public final class DiffPositionMapper {

    private final Map<String, Integer> index = new HashMap<>();

    public static DiffPositionMapper parse(String diff) {
        DiffPositionMapper mapper = new DiffPositionMapper();
        mapper.doParse(diff);
        return mapper;
    }

    private void doParse(String diff) {
        String currentFile = null;
        int diffPosition = 0;
        int newFileLine = 0;

        for (String line : diff.split("\n")) {
            if (line.startsWith("+++ b/")) {
                currentFile = line.substring(6);
                diffPosition = 0;
                continue;
            }

            if (line.startsWith("diff --git") || line.startsWith("--- ") || line.startsWith("index ")) {
                continue;
            }

            if (line.startsWith("@@")) {
                diffPosition = 0;
                String[] parts = line.split("\\+")[1].split(",")[0].trim().split(" ")[0].split("@@")[0].trim().split("[,@]");
                newFileLine = Integer.parseInt(parts[0]) - 1;
                continue;
            }

            if (currentFile == null) {
                continue;
            }

            diffPosition++;

            if (line.startsWith("-")) {
                continue;
            }

            newFileLine++;

            if (line.startsWith("+") || line.startsWith(" ")) {
                index.put(currentFile + ":" + newFileLine, diffPosition);
            }
        }
    }

    public int getDiffPosition(String path, int fileLineNumber) {
        return index.getOrDefault(path + ":" + fileLineNumber, -1);
    }

    public static java.util.Set<String> parseFileNames(String diff) {
        java.util.Set<String> files = new java.util.LinkedHashSet<>();
        for (String line : diff.split("\n")) {
            if (line.startsWith("+++ b/")) {
                files.add(line.substring(6));
            }
        }
        return files;
    }
}
