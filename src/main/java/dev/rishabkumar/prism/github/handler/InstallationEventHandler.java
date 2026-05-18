package dev.rishabkumar.prism.github.handler;

import dev.rishabkumar.prism.github.service.InstallationService;
import io.quarkiverse.githubapp.event.Installation;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;

public class InstallationEventHandler {

    @Inject
    InstallationService installationService;

    void onInstalled(@Installation.Created GHEventPayload.Installation payload) {
        long installationId = payload.getInstallation().getId();
        String accountName = payload.getInstallation().getAccount() != null
                ? payload.getInstallation().getAccount().getLogin()
                : "unknown";
        installationService.onInstall(installationId, accountName);
    }

    void onUninstalled(@Installation.Deleted GHEventPayload.Installation payload) {
        long installationId = payload.getInstallation().getId();
        String accountName = payload.getInstallation().getAccount() != null
                ? payload.getInstallation().getAccount().getLogin()
                : "unknown";
        installationService.onUninstall(installationId, accountName);
    }
}
