package pl.tomaszko.s04e02.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

@Component
public class ModelCommunicationLogger implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger("pl.tomaszko.s04e02.llm");

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        if (request.prompt() != null && request.prompt().getInstructions() != null) {
            for (Message message : request.prompt().getInstructions()) {
                log.info("model request type={} content={}", message.getMessageType(), message.getText());
            }
        }
        if (request.prompt() != null && request.prompt().getOptions() != null) {
            log.info("model request options={}", request.prompt().getOptions());
        }
        ChatClientResponse response = chain.nextCall(request);
        if (response.chatResponse() != null && response.chatResponse().getResult() != null
                && response.chatResponse().getResult().getOutput() != null) {
            log.info("model response={}", response.chatResponse().getResult().getOutput().getText());
        }
        return response;
    }

    @Override
    public String getName() {
        return "ModelCommunicationLogger";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
