package com.AI_assistant.Controller;

import com.AI_assistant.Models.ChatMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.naming.directory.SearchResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class EntryResource {

    private final OllamaChatModel chatClient;

    private final VectorStore vectorStore;

    private final String prompt = """
            You are a knowledgeable assistant trained on the Below document.
            
            Your task is to answer any QUESTION asked by the user, strictly using only the content from the DOCUMENTS section.
            
            Follow these instructions:
            
            1. If the question asks for a summary, limit your response to the number of words specified (e.g., "Summarize in 25 words").
            2. If the user asks for a list or bullet points, format your response accordingly.
            3. If the answer is not found in the DOCUMENTS, respond with: "I'm not sure about that based on the provided documents."
            4. Be concise, clear, and factual.
            
            ---
            QUESTION:
            {input}
            
            DOCUMENTS:
            {documents}
            """;

    @Autowired
    public EntryResource(OllamaChatModel chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/conversation")
    public String answer(@RequestParam String userInput, @RequestParam(required = false) String source, HttpSession session) {

        List<ChatMessage> messages = ChatController.getMessagesFromSession(session);
        messages.add(new ChatMessage("user", userInput));

        // Send only last N messages to the AI
        int maxMessages = 10; // You can tune this based on your needs
        List<ChatMessage> recentMessages = messages.subList(
                Math.max(messages.size() - maxMessages, 0),
                messages.size()
        );

        StringBuilder context = new StringBuilder();
        for (ChatMessage msg : recentMessages) {
            context.append(msg.sender).append(": ").append(msg.text).append("\n");
        }

        PromptTemplate template = new PromptTemplate(prompt);

        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", context.toString());
        promptParameters.put("documents", findSimilarData(context.toString(), source));

        String llmResponse = chatClient.call(template.createMessage(promptParameters));
        messages.add(new ChatMessage("ai", llmResponse));

        return llmResponse;
    }

    private String findSimilarData(String message, String source) {

        String filterExpr = "source == '" + source + "'";
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .filterExpression(source != null ? filterExpr : null)
                .build());
        System.out.println(documents.size());
        String response = documents.stream().map(Document::getFormattedContent).collect(Collectors.joining("/n"));
        System.out.println(response);

         /* Alternative
         return documents.stream()
    .limit(5)
    .map(Document::getFormattedContent)
    .collect(Collectors.joining("\n"));
          */

        return response;
    }

}
