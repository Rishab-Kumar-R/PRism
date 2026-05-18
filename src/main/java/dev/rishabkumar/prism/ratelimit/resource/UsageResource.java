package dev.rishabkumar.prism.ratelimit.resource;

import dev.rishabkumar.prism.exception.ReviewNotFoundException;
import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import dev.rishabkumar.prism.ratelimit.model.UsageResponse;
import dev.rishabkumar.prism.ratelimit.repository.DailyUsageRepository;
import dev.rishabkumar.prism.ratelimit.repository.InstallationConfigRepository;
import dev.rishabkumar.prism.ratelimit.repository.MonthlyUsageRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;

@Path("/usage")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Usage", description = "Query installation usage and rate limit status")
public class UsageResource {

    @Inject
    InstallationConfigRepository installationConfigRepository;

    @Inject
    MonthlyUsageRepository monthlyUsageRepository;

    @Inject
    DailyUsageRepository dailyUsageRepository;

    @GET
    @Path("/{installationId}")
    @Transactional
    @Operation(summary = "Get usage for an installation",
            description = "Returns current tier, monthly and daily review usage, budget, and reset dates")
    @APIResponse(responseCode = "200", description = "Current usage for the installation")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Installation not found")
    public UsageResponse getUsage(
            @Parameter(description = "GitHub App installation ID") @PathParam("installationId") long installationId) {

        InstallationConfig config = installationConfigRepository
                .findByInstallation(installationId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "No usage data found for installation: " + installationId));

        if (!config.active) {
            throw new ReviewNotFoundException("Installation " + installationId + " has been uninstalled");
        }

        LocalDate today = LocalDate.now();

        long monthlyUsed = monthlyUsageRepository
                .findByInstallationPeriod(installationId, today.getYear(), today.getMonthValue())
                .map(u -> u.reviewCount)
                .orElse(0L);

        long dailyUsed = dailyUsageRepository
                .findByInstallationDay(installationId, today.getYear(), today.getMonthValue(), today.getDayOfMonth())
                .map(u -> u.reviewCount)
                .orElse(0L);

        return UsageResponse.from(config, monthlyUsed, dailyUsed);
    }
}
