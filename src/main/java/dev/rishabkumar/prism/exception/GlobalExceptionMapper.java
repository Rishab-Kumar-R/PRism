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
