package pl.tomaszko.s04e02.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.tomaszko.s04e02.logging.ModelCommunicationLogger;
import pl.tomaszko.s04e02.tools.PlantTool;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            ModelCommunicationLogger modelCommunicationLogger,
            PlantTool plantTool
    ) {
        return builder
                .defaultAdvisors(modelCommunicationLogger)
                .defaultTools(plantTool)
                .build();
    }
}
