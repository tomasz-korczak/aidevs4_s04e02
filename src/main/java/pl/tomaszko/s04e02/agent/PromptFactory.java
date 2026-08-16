package pl.tomaszko.s04e02.agent;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.config.AppProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptFactory {

    private final AppProperties appProperties;
    private final ResourceLoader resourceLoader;

    public PromptFactory(AppProperties appProperties, ResourceLoader resourceLoader) {
        this.appProperties = appProperties;
        this.resourceLoader = resourceLoader;
    }

    public String systemPrompt() {
        String location = appProperties.getPrompt().getSystemTemplateLocation();
        Resource resource = resourceLoader.getResource(location);
        try {
            String template = resource.getContentAsString(StandardCharsets.UTF_8);
            return template
                    .replace("{{maxAttempts}}", String.valueOf(appProperties.getPlant().getMaxSessionAttempts()))
                    .replace("{{model}}", appProperties.getLlm().getModel())
                    .replace("{{verifyUrl}}", appProperties.getHub().getVerifyUrl());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load system prompt from " + location, e);
        }
    }
}
