package dev.rishabkumar.prism.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrismConfig {

    private ReviewConfig review = new ReviewConfig();

    public ReviewConfig getReview() {
        return review;
    }

    public void setReview(ReviewConfig review) {
        this.review = review;
    }

    public static PrismConfig defaults() {
        return new PrismConfig();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReviewConfig {

        private List<String> ignore = List.of();
        private List<String> focus = List.of("all");
        private String tone = "detailed";
        private String instructions = "";

        public List<String> getIgnore() {
            return ignore;
        }

        public void setIgnore(List<String> ignore) {
            this.ignore = ignore;
        }

        public List<String> getFocus() {
            return focus;
        }

        public void setFocus(List<String> focus) {
            this.focus = focus;
        }

        public String getTone() {
            return tone;
        }

        public void setTone(String tone) {
            this.tone = tone;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }
    }
}
