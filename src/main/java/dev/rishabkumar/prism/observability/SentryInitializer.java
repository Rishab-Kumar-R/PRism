package dev.rishabkumar.prism.observability;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.sentry.Sentry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class SentryInitializer {

    @ConfigProperty(name = "sentry.dsn")
    Optional<String> dsn;

    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    String profile;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "unknown")
    String version;

    void onStart(@Observes StartupEvent event) {
        if (dsn.isEmpty() || dsn.get().isBlank()) {
            Log.info("Sentry DSN not configured - error tracking disabled");
            return;
        }

        Sentry.init(options -> {
            options.setDsn(dsn.get());
            options.setEnvironment(profile);
            options.setRelease(version);
            options.setTracesSampleRate(0.0);
        });

        Log.infof("Sentry initialised (env=%s, release=%s)", profile, version);
    }
}
