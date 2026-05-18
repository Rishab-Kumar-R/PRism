package dev.rishabkumar.prism.github.service;

import dev.rishabkumar.prism.ratelimit.repository.InstallationConfigRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class InstallationService {

    @Inject
    InstallationConfigRepository installationConfigRepository;

    @Transactional
    public void onInstall(long installationId, String accountName) {
        installationConfigRepository.getOrCreateDefault(installationId, accountName);
        Log.infof("GitHub App installed: installationId=%d account=%s", installationId, accountName);
    }

    @Transactional
    public void onUninstall(long installationId, String accountName) {
        installationConfigRepository.deactivate(installationId);
        Log.infof("GitHub App uninstalled: installationId=%d account=%s", installationId, accountName);
    }
}
