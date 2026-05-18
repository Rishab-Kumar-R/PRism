package dev.rishabkumar.prism.exception;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        if (e instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        if (e instanceof ReviewNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Not found", "message", e.getMessage()))
                    .build();
        }

        if (e instanceof PrAlreadyPausedException || e instanceof PrNotPausedException) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Conflict", "message", e.getMessage()))
                    .build();
        }

        if (e instanceof DiffFetchException) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Bad gateway", "message", e.getMessage()))
                    .build();
        }

        if (e instanceof AIReviewException) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Bad gateway", "message", e.getMessage()))
                    .build();
        }

        if (e instanceof RateLimitExceededException) {
            return Response.status(429)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Too Many Requests", "message", e.getMessage()))
                    .build();
        }

        Log.errorf(e, "Unhandled exception: %s", e.getMessage());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage() != null ? e.getMessage() : "Unexpected error"
                ))
                .build();
    }
}
