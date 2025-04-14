package com.AI_assistant.Controller;

import com.AI_assistant.Constants.ChatConstant;
import com.AI_assistant.Models.ChatMessage;
import com.AI_assistant.Models.ChatSessionState;
import com.AI_assistant.Models.ValidOptionDto;
import com.AI_assistant.Utils.UserSuggestionsUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.messages.Message;
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

    private final UserSuggestionsUtil userSuggestionsUtil;

    private final String prompt = """
            You are an AI assistant with access ONLY to the provided DOCUMENTS.
            
            Your task is to answer the user's QUESTION strictly using the DOCUMENTS and the CONVERSATION SUMMARY. Do not use external knowledge under any circumstances.
            
            Rules to follow:
            
            1. If the user greets you (e.g., "Hi", "Hello", etc.), respond politely and ask them to ask a question about the DOCUMENTS.
            2. If the user asks a factual question, answer ONLY if the answer is explicitly present in the DOCUMENTS.
            3. If the user asks for a summary, respond with a concise summary using only information from the DOCUMENTS.
            4. If the user requests a list or bullet points, format the response using data from the DOCUMENTS.
            5. If the DOCUMENTS do NOT contain the answer, reply exactly with:
            "I'm not sure about that based on the provided documents."
            
            ❗ Do NOT answer based on general knowledge or assumptions.
            ❗ Do NOT include additional information not in the DOCUMENTS.
            ❗ Do NOT be overly helpful — stick strictly to the DOCUMENTS.
            
            ---
            
            CONVERSATION SUMMARY:
            {summary}
            
            QUESTION:
            {input}
            
            DOCUMENTS:
            {documents}
            """;

    private final String junitPrompt = """
            You are an expert Java developer and test engineer. Your task is to write high-quality, production-ready unit test cases using JUnit 5 for the given Java method or class. Follow these strict guidelines:
            
            1. Use JUnit 5 (`@Test`, `Assertions`, etc.) for all tests.
            2. If dependencies exist, use Mockito to mock them.
            3. Ensure all logic branches (if-else/switch/catch/etc.) are tested.
            4. Use clear and descriptive test method names following the pattern:
               testMethodName_condition_expectedResult
            5. Include edge cases and negative scenarios.
            6. Avoid unnecessary boilerplate; focus on clarity and correctness.
            7. Annotate the test class with `@ExtendWith(MockitoExtension.class)` when using mocks.
            8. Ensure tests are **isolated**, **repeatable**, and **independent**.
            9. Do **not** rewrite or modify the input class/method — only generate test cases.
            10. If method relies on external classes, assume they are injectable and mockable.
            
            ### Input Java Code:
            {input}
            """;


    @Autowired
    public EntryResource(OllamaChatModel chatClient, VectorStore vectorStore, UserSuggestionsUtil userSuggestionsUtil) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.userSuggestionsUtil = userSuggestionsUtil;
    }

    @PostMapping("/conversation")
    public ChatMessage answer(@RequestParam String userInput, HttpSession session) {

        System.out.println("Request Initiated!");
        userInput = userInput.trim();
        ChatSessionState chatSession = ChatController.getOrInitChatSession(session);
        String optionSelected = (String) session.getAttribute("optionSelected");
        String source = (String) session.getAttribute("source");
        List<ChatMessage> messages = chatSession.getMessages();
        ChatMessage currentMessage = new ChatMessage();
        if (optionSelected == null || optionSelected.isBlank()) {
            String automatedResponse = "";
            messages.add(new ChatMessage("user", userInput, ChatConstant.AUTOMATED_MESSAGE_TYPE));
            ValidOptionDto validOptionOrNot = userSuggestionsUtil.isValidOption(userInput);
            if (validOptionOrNot.isValid() && ChatConstant.OPTION_TYPE_JUNIT.equalsIgnoreCase(validOptionOrNot.getOptionType())) {
                session.setAttribute("optionSelected", userInput);
                session.setAttribute("source", validOptionOrNot.getSource());
                automatedResponse = "You're all set for JUNIT. Share your Java Method or Class Code.";
                updateChatMessageWithDetails(currentMessage,"ai",automatedResponse,null,ChatConstant.AUTOMATED_MESSAGE_TYPE);
                messages.add(currentMessage);
            } else if (validOptionOrNot.isValid()) {
                session.setAttribute("optionSelected", userInput);
                session.setAttribute("source", validOptionOrNot.getSource());
                automatedResponse = "Great! You selected '" + userInput + "'. Ask your question now.";
                updateChatMessageWithDetails(currentMessage,"ai",automatedResponse,null,ChatConstant.AUTOMATED_MESSAGE_TYPE);
                messages.add(currentMessage);
            } else {
                automatedResponse = "Invalid Input : <br/>" + String.join("<br/>", userSuggestionsUtil.getAvailableOptions());
                updateChatMessageWithDetails(currentMessage,"ai",automatedResponse,null,ChatConstant.AUTOMATED_MESSAGE_TYPE);
                messages.add(currentMessage);
            }
            return currentMessage;
        }

        if (ChatConstant.OPTION_TYPE_JUNIT.equalsIgnoreCase(source)) {
            System.out.println("Request is for JUNIT Generation!");
            return JUnitResponse(chatSession, userInput);
        }

        updateChatMessageWithDetails(currentMessage,"user",userInput,null,ChatConstant.CONVERSATION_MESSAGE_TYPE);
        messages.add(new ChatMessage("user",userInput,ChatConstant.CONVERSATION_MESSAGE_TYPE));

        PromptTemplate template = new PromptTemplate(prompt);

        List<ChatMessage> contextForLlm = getContextMessages(chatSession);
        Message msg = buildPrompt(chatSession.getSummary(), contextForLlm, userInput, source, template);
        String llmResponse = chatClient.call(msg);

        updateChatMessageWithDetails(currentMessage,"ai",llmResponse,null,ChatConstant.CONVERSATION_MESSAGE_TYPE);
        messages.add(currentMessage);

        if (chatSession.getMessages().size() > ChatConstant.SUMMARY_THRESHOLD) {
            chatSession.setSummary(summarizeOldMessages(chatSession.getMessages()));
        }
        System.out.println("Request Completed!");
        return currentMessage;
    }

    private String findSimilarData(String message, String source) {

        String filterExpr = "source == '" + source + "'";
        System.out.println(filterExpr);
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .filterExpression(source != null ? filterExpr : null)
                .build());
        for (Document doc : documents) {
            doc.getMetadata().forEach((k, v) -> System.out.println(k + " = " + v));
            System.out.println("Matched: " + doc.getMetadata().get("source"));
        }
        System.out.println("Similarity Search Data Size : " + documents.size());

        return documents.stream().map(Document::getFormattedContent).collect(Collectors.joining("\n"));
    }

    private String summarizeOldMessages(List<ChatMessage> all) {
        List<ChatMessage> toSummarize = all.subList(0, Math.min(ChatConstant.SUMMARY_THRESHOLD, all.size()));
        StringBuilder chatBlock = new StringBuilder();
        for (ChatMessage msg : toSummarize) {
            chatBlock.append(msg.getSender()).append(": ").append(msg.getText()).append("\n");
        }
        String summarizationPrompt = "Summarize the following conversation in 2-3 sentences:\n" + chatBlock;
        return chatClient.call(summarizationPrompt);
    }

    private List<ChatMessage> getContextMessages(ChatSessionState session) {
        List<ChatMessage> all = session.getMessages().stream()
                .filter(msg -> ChatConstant.CONVERSATION_MESSAGE_TYPE.equals(msg.getType())).toList();
        int start = Math.max(0, all.size() - ChatConstant.CONTEXT_SIZE);
        return all.subList(start, all.size());
    }

    private Message buildPrompt(String summary, List<ChatMessage> context,
                                String currentUserInput, String source,
                                PromptTemplate template) {

        Map<String, Object> promptParameters = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : context) {
            sb.append(msg.getSender()).append(": ").append(msg.getText()).append("\n");
        }
        promptParameters.put("input", sb.toString());

        String contextText = context.stream()
                .map(m -> m.getSender() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
        promptParameters.put("documents", findSimilarData(contextText, source));
        if (summary != null && !summary.isEmpty()) {
            promptParameters.put("summary", summary);
        } else {
            promptParameters.put("summary", "");
        }
        return template.createMessage(promptParameters);
    }


    private ChatMessage JUnitResponse(ChatSessionState chatSession, String userInput) {
        List<ChatMessage> messages = chatSession.getMessages();
        PromptTemplate template = new PromptTemplate(junitPrompt);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", userInput);
        String llmResponse = chatClient.call(template.createMessage(promptParameters));
        ChatMessage message = new ChatMessage();
        segregateLlmResponse(llmResponse, message, "ai", ChatConstant.CONVERSATION_MESSAGE_TYPE);
        messages.add(message);
        System.out.println("JUNIT Response Completed");
        return message;
    }

    private void segregateLlmResponse(String llmResponse, ChatMessage message, String sender, String messageType) {
        message.setSender(sender);
        message.setType(messageType);
        message.setText(llmResponse);
        System.out.println("Printing Response :: 223 :: \n"+llmResponse);
        if (llmResponse != null && !llmResponse.isBlank() && llmResponse.contains("```")) {
            System.out.println("Response has Code snippet");
            String plainText = llmResponse;
            String codeSnippet = null;

            int start = llmResponse.indexOf("```");
            int end = llmResponse.indexOf("```", start + 3);

            if (start >= 0 && end > start) {
                codeSnippet = llmResponse.substring(start + 3, end).trim();
                plainText = llmResponse.substring(0, start).trim()+"\n "+llmResponse.substring(end+3);
            }
            message.setText(plainText);
            System.out.println("PalineText At 237 :: \n"+plainText);
            message.setCodeSnippet(codeSnippet);
            System.out.println("CodeSnippet At 239 :: \n"+codeSnippet);
        }
    }

    private void updateChatMessageWithDetails(ChatMessage currentMessage,String sender,String text,String codeSnippet,String messageType){
        currentMessage.setSender(sender);
        currentMessage.setCodeSnippet(codeSnippet);
        currentMessage.setText(text);
        currentMessage.setType(messageType);
    }

}
