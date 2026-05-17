package dev.rishabkumar.prism.review;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/reviews")
@Produces(MediaType.APPLICATION_JSON)
public class ReviewResource {

    @Inject
    ReviewService reviewService;

    @GET
    public List<ReviewRecord> all(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getAll(page, size);
    }

    @GET
    @Path("/{id}")
    public ReviewRecord byId(@PathParam("id") Long id) {
        return reviewService.getById(id);
    }

    @GET
    @Path("/repo/{repoName}")
    public List<ReviewRecord> byRepo(
            @PathParam("repoName") String repoName,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getByRepo(repoName, page, size);
    }

    @GET
    @Path("/pr/{prNumber}")
    public List<ReviewRecord> byPr(
            @PathParam("prNumber") int prNumber,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getByPr(prNumber, page, size);
    }

    @GET
    @Path("/repo/{repoName}/pr/{prNumber}")
    public List<ReviewRecord> byRepoAndPr(
            @PathParam("repoName") String repoName,
            @PathParam("prNumber") int prNumber,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return reviewService.getByRepoAndPr(repoName, prNumber, page, size);
    }

    @GET
    @Path("/stats")
    public ReviewStats stats() {
        return reviewService.getStats();
    }
}
