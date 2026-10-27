package com.clausify.analysis;

import com.clausify.analysis.dto.ContractAnalysisResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.converter.*;
import org.springframework.stereotype.Service;

@Service
public class GroqContractAnalyzer {

    private final ChatClient chatClient;
    private final BeanOutputConverter<ContractAnalysisResponse> outputConverter;

    public GroqContractAnalyzer(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(ContractAnalysisResponse.class);
    }

    public ContractAnalysisResponse analyzeContractText(String contractText) {
        String systemPrompt = """
            You are an expert contract review AI specialized in helping freelancers spot risk.
            Analyze the following contract text for risky, unfair, or highly favorable clauses.
            Identify issues falling into standard risk categories (such as Liability, Payment, Intellectual Property, Termination).
            
            For each finding:
            1. Extract the exact text 'originalText' from the document.
            2. Classify risk level as 'High' (uncapped liability, indemnification traps), 'Medium', 'Low', or 'Favorable' (late fees for unpaid work, short payment terms).
            3. Provide a clear, jargon-free explanation.
            4. Suggest a direct alternative text revision that protects the freelancer.
            
            {format}
            """;
            //FIXME: ^^this was a basic prompt that I had AI generate.
            //May need to be improved, I'm not very good with AI. -Rival
        return this.chatClient.prompt()
                .system(s -> s.text(systemPrompt).param("format", outputConverter.getFormat()))
                .user(u -> u.text("Analyze this contract text:\n\n" + contractText))
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .entity(outputConverter); // Matches format constraints to the parser cleanly
    }
}
