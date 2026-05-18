package dev.rishabkumar.prism.admin.resource;

import dev.rishabkumar.prism.admin.model.InstallationSummary;
import dev.rishabkumar.prism.admin.model.UpdateLimitsRequest;
import dev.rishabkumar.prism.admin.model.UpdateTierRequest;
import dev.rishabkumar.prism.exception.ReviewNotFoundException;
import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import dev.rishabkumar.prism.ratelimit.repository.InstallationConfigRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/admin/installations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Manage installation tiers and limits")
public class AdminResource {

    @Inject
    InstallationConfigRepository installationConfigRepository;

    @GET
    @Transactional
    @Operation(summary = "List all installations",
            description = "Returns all known installations. Filter by active status with ?active=true/false")
    @APIResponse(responseCode = "200", description = "List of installations")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    public List<InstallationSummary> listInstallations(
            @Parameter(description = "Filter by active status") @QueryParam("active") Boolean active) {
        List<InstallationConfig> configs;
        if (active != null) {
            configs = installationConfigRepository.find("active", active).list();
        } else {
            configs = installationConfigRepository.listAll();
        }
        return configs.stream().map(InstallationSummary::from).toList();
    }

    @GET
    @Path("/{installationId}")
    @Transactional
    @Operation(summary = "Get a single installation")
    @APIResponse(responseCode = "200", description = "Installation details")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Installation not found")
    public InstallationSummary getInstallation(
            @Parameter(description = "GitHub App installation ID") @PathParam("installationId") long installationId) {
        return installationConfigRepository.findByInstallation(installationId)
                .map(InstallationSummary::from)
                .orElseThrow(() -> new ReviewNotFoundException("Installation not found: " + installationId));
    }

    @PATCH
    @Path("/{installationId}/tier")
    @Transactional
    @Operation(summary = "Update installation tier",
            description = "Upgrade or downgrade the tier for an installation")
    @APIResponse(responseCode = "200", description = "Updated installation")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Installation not found")
    public InstallationSummary updateTier(
            @Parameter(description = "GitHub App installation ID") @PathParam("installationId") long installationId,
            UpdateTierRequest request) {
        if (request == null || request.tier() == null) {
            throw new jakarta.ws.rs.BadRequestException("tier is required");
        }
        InstallationConfig config = installationConfigRepository.findByInstallation(installationId)
                .orElseThrow(() -> new ReviewNotFoundException("Installation not found: " + installationId));
        config.tier = request.tier();
        return InstallationSummary.from(config);
    }

    @PATCH
    @Path("/{installationId}/limits")
    @Transactional
    @Operation(summary = "Set custom review limits",
            description = "Override the tier defaults with custom monthly and/or daily limits. Pass null to clear a custom limit and revert to tier default.")
    @APIResponse(responseCode = "200", description = "Updated installation")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Installation not found")
    public InstallationSummary updateLimits(
            @Parameter(description = "GitHub App installation ID") @PathParam("installationId") long installationId,
            UpdateLimitsRequest request) {
        InstallationConfig config = installationConfigRepository.findByInstallation(installationId)
                .orElseThrow(() -> new ReviewNotFoundException("Installation not found: " + installationId));
        config.customMonthlyReviewLimit = request.customMonthlyReviewLimit();
        config.customDailyReviewLimit = request.customDailyReviewLimit();
        return InstallationSummary.from(config);
    }
}
