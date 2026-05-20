package dev.rishabkumar.prism.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rishabkumar.prism.config.model.PrismConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@ApplicationScoped
public class RepoConfigService {

    @Inject
    ObjectMapper objectMapper;

    public PrismConfig getConfig(String repoFullName, GHRepository repository) {
        try {
            GHContent content = repository.getFileContent(".prism.yaml");
            if (content == null) {
                return PrismConfig.defaults();
            }

            try (InputStream in = content.read()) {
                Yaml yaml = new Yaml();
                Map<String, Object> raw = yaml.load(in);
                PrismConfig config = objectMapper.convertValue(raw, PrismConfig.class);
                Log.infof("[%s] Loaded .prism.yaml config", repoFullName);
                return config;
            }
        } catch (Exception e) {
            Log.debugf("[%s] No .prism.yaml found, using defaults", repoFullName);
            return PrismConfig.defaults();
        }
    }
}
