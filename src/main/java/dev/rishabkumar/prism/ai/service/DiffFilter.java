package dev.rishabkumar.prism.ai.service;

import java.util.List;
import java.util.regex.Pattern;

public final class DiffFilter {

    private DiffFilter() {}

    public static String apply(String diff, List<String> ignorePatterns) {
        if (ignorePatterns == null || ignorePatterns.isEmpty()) {
            return diff;
        }

        List<Pattern> patterns = ignorePatterns.stream()
                .map(DiffFilter::globToRegex)
                .toList();

        List<String> fileDiffs = DiffSplitter.splitByFile(diff);
        StringBuilder result = new StringBuilder();

        for (String fileDiff : fileDiffs) {
            String fileName = extractFileName(fileDiff);
            boolean ignored = patterns.stream().anyMatch(p -> p.matcher(fileName).matches());
            if (!ignored) {
                result.append(fileDiff);
            }
        }

        return result.isEmpty() ? diff : result.toString();
    }

    private static String extractFileName(String fileDiff) {
        for (String line : fileDiff.split("\n")) {
            if (line.startsWith("+++ b/")) {
                return line.substring(6);
            }
        }
        return "";
    }

    private static Pattern globToRegex(String glob) {
        String regex = glob
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace("?", "[^/]");
        return Pattern.compile(regex);
    }
}
