package com.taskapi.ai;

import com.taskapi.ai.AiDTO.AiRequest;
import com.taskapi.ai.AiDTO.AiResponse;
import com.taskapi.ai.AiDTO.AiStatusResponse;
import com.taskapi.ai.AiProperties.AiConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class AiService {

    private static final Set<String> VALID_DOMAINS =
        Set.of("contabilidade", "financas", "administracao", "matematica");

    private final AiProvider provider;
    private final AiConfig config;

    public AiService(AiProvider provider, AiConfig config) {
        this.provider = provider;
        this.config = config;
    }

    public AiResponse ask(AiRequest request) throws IOException {
        if (!VALID_DOMAINS.contains(request.domain())) {
            throw new IllegalArgumentException(
                "Domínio inválido: " + request.domain() +
                ". Válidos: " + VALID_DOMAINS
            );
        }

        String systemPrompt = loadPrompt(request.domain());
        String answer = provider.chat(systemPrompt, request.question());

        return new AiResponse(answer, config.provider() + "/" + config.model());
    }

    public AiStatusResponse status() {
        return new AiStatusResponse(
            provider.isEnabled(),
            provider.isEnabled() ? config.provider() + "/" + config.model() : null
        );
    }

    private String loadPrompt(String domain) throws IOException {
        var resource = new ClassPathResource("prompts/" + domain + ".md");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}