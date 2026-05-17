package dev.rishabkumar.prism.ai;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SummaryService {

    @Inject
    SummaryAI summaryAI;

    public String summarize(String diff) {
        if (diff == null || diff.isBlank()) {
            Log.warn("Empty diff received for summary, skipping");
            return null;
        }

        Log.infof("Sending %d char diff to AI for summary", diff.length());
        String summary = summaryAI.summarize(diff);

        if (summary == null || summary.isBlank()) {
            Log.warn("AI returned empty summary");
            return null;
        }

        Log.info("AI summary generated successfully");
        return summary;
    }
}
