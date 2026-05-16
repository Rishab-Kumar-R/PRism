package dev.rishabkumar.review;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/reviews")
@Produces(MediaType.APPLICATION_JSON)
public class ReviewResource {

    @Inject
    ReviewRepository reviewRepository;

    @GET
    public List<ReviewRecord> all() {
        return reviewRepository.listAll();
    }

    @GET
    @Path("/repo/{repoName}")
    public List<ReviewRecord> byRepo(@PathParam("repoName") String repoName) {
        return reviewRepository.findByRepo(repoName);
    }

    @GET
    @Path("/pr/{prNumber}")
    public List<ReviewRecord>  byPr(@PathParam("prNumber") int prNumber) {
        return reviewRepository.findByPrNumber(prNumber);
    }
}
