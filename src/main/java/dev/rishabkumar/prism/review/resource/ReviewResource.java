package dev.rishabkumar.prism.review.resource;

import dev.rishabkumar.prism.review.model.ReviewRecord;
import dev.rishabkumar.prism.review.model.ReviewStats;
import dev.rishabkumar.prism.review.model.ReviewSummary;
import dev.rishabkumar.prism.review.service.ReviewService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reviews", description = "Query AI code review history and stats")
public class ReviewResource {

    @Inject
    ReviewService reviewService;

    @GET
    @Operation(summary = "List all reviews", description = "Returns paginated list of all reviews without full review text")
    @APIResponse(responseCode = "200", description = "List of review summaries")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    public List<ReviewSummary> all(
            @Parameter(description = "Page number, zero-based") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getAll(page, size);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get review by ID", description = "Returns full review including the complete AI review markdown")
    @APIResponse(responseCode = "200", description = "Full review record",
            content = @Content(schema = @Schema(implementation = ReviewRecord.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Review not found")
    public ReviewRecord byId(@Parameter(description = "Review ID") @PathParam("id") Long id) {
        return reviewService.getById(id);
    }

    @GET
    @Path("/repo/{repoName}")
    @Operation(summary = "List reviews by repository", description = "Returns all reviews for a given repository, newest first")
    @APIResponse(responseCode = "200", description = "List of review summaries for the repo")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    public List<ReviewSummary> byRepo(
            @Parameter(description = "Full repo name e.g. owner/repo") @PathParam("repoName") String repoName,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getByRepo(repoName, page, size);
    }

    @GET
    @Path("/pr/{prNumber}")
    @Operation(summary = "List reviews by PR number")
    @APIResponse(responseCode = "200", description = "List of review summaries for the PR")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    public List<ReviewSummary> byPr(
            @Parameter(description = "PR number") @PathParam("prNumber") int prNumber,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getByPr(prNumber, page, size);
    }

    @GET
    @Path("/repo/{repoName}/pr/{prNumber}")
    @Operation(summary = "List reviews for a specific PR in a repository", description = "Returns all reviews for a specific PR, newest first")
    @APIResponse(responseCode = "200", description = "List of review summaries")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    public List<ReviewSummary> byRepoAndPr(
            @Parameter(description = "Full repo name e.g. owner/repo") @PathParam("repoName") String repoName,
            @Parameter(description = "PR number") @PathParam("prNumber") int prNumber,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getByRepoAndPr(repoName, prNumber, page, size);
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get review statistics", description = "Returns aggregated stats: totals, approval rate, average score, most common issue type")
    @APIResponse(responseCode = "200", description = "Aggregated review statistics",
            content = @Content(schema = @Schema(implementation = ReviewStats.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    public ReviewStats stats() {
        return reviewService.getStats();
    }
}
