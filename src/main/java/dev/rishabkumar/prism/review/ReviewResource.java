package dev.rishabkumar.prism.review;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/reviews")
@Produces(MediaType.APPLICATION_JSON)
public class ReviewResource {

    @Inject
    ReviewRepository reviewRepository;

    @GET
    public List<ReviewRecord> all(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewRepository.findAll().page(page, size).list();
    }

    @GET
    @Path("/{id}")
    public Response byId(@PathParam("id") Long id) {
        ReviewRecord record = reviewRepository.findById(id);

        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(record).build();
    }

    @GET
    @Path("/repo/{repoName}")
    public List<ReviewRecord> byRepo(
            @PathParam("repoName") String repoName,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewRepository.find("repoName", repoName).page(page, size).list();
    }

    @GET
    @Path("/pr/{prNumber}")
    public List<ReviewRecord> byPr(
            @PathParam("prNumber") int prNumber,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewRepository.find("prNumber", prNumber).page(page, size).list();
    }

    @GET
    @Path("/stats")
    public ReviewStats stats() {
        return new ReviewStats(
                reviewRepository.count(),
                reviewRepository.countApproved(),
                reviewRepository.countNeedsWork(),
                reviewRepository.averageScore(),
                reviewRepository.totalBugs(),
                reviewRepository.totalSecurityIssues(),
                reviewRepository.totalPerformanceIssues(),
                reviewRepository.mostReviewedRepo()
        );
    }
}
