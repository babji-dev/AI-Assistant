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
    You are a helpful and knowledgeable assistant trained on the below documents.

    Your task is to answer the user's QUESTION using the DOCUMENTS and previous CONVERSATION SUMMARY for context.

    Please follow these rules:

    1. If the user greets you (e.g., "Hi", "Hello", "Good morning", etc.), respond politely and invite them to ask a question about the below Mentioned documents.
    2. If the user asks a factual question, answer using only the content from the DOCUMENTS section.
    3. If the question asks for a summary, limit your response to the word count specified (e.g., "Summarize in 25 words").
    4. If the user asks for a list or bullet points, format your response accordingly.
    5. If the answer is not found in the DOCUMENTS, say: "I'm not sure about that based on the provided documents."
    6. Be concise, clear, and factual.

    ---
    CONVERSATION SUMMARY:
    {summary}

    QUESTION:
    {input}

    DOCUMENTS:
    {documents}
    """;


    @Autowired
    public EntryResource(OllamaChatModel chatClient, VectorStore vectorStore,UserSuggestionsUtil userSuggestionsUtil) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.userSuggestionsUtil = userSuggestionsUtil;
    }

    @PostMapping("/conversation")
    public String answer(@RequestParam String userInput, HttpSession session) {

        ChatSessionState chatSession = ChatController.getOrInitChatSession(session);
        String optionSelected = (String) session.getAttribute("optionSelected");
        String source = (String) session.getAttribute("source");
        List<ChatMessage> messages = chatSession.getMessages();

        if(optionSelected == null || optionSelected.isBlank()) {
            String automatedResponse = "";
            messages.add(new ChatMessage("user",userInput,ChatConstant.AUTOMATED_MESSAGE_TYPE));
            ValidOptionDto validOptionOrNot = userSuggestionsUtil.isValidOption(userInput);
            if (validOptionOrNot.isValid()) {
                session.setAttribute("optionSelected", userInput);
                session.setAttribute("source",validOptionOrNot.getSource());
                automatedResponse =  "Great! You selected '" + userInput + "'. Ask your question now.";
                messages.add(new ChatMessage("ai",userInput,ChatConstant.AUTOMATED_MESSAGE_TYPE));
            } else {
                automatedResponse = "Invalid Input : <br/>" + String.join("<br/>", userSuggestionsUtil.getAvailableOptions());
                messages.add(new ChatMessage("ai",automatedResponse,ChatConstant.AUTOMATED_MESSAGE_TYPE));
            }
            return automatedResponse;
        }

        messages.add(new ChatMessage("user", userInput, ChatConstant.CONVERSATION_MESSAGE_TYPE));

        PromptTemplate template = new PromptTemplate(prompt);

        List<ChatMessage> contextForLlm = getContextMessages(chatSession);
        Message msg = buildPrompt(chatSession.getSummary(), contextForLlm, userInput, source,template);
        String llmResponse = chatClient.call(msg);

        messages.add(new ChatMessage("ai", llmResponse,ChatConstant.CONVERSATION_MESSAGE_TYPE));

        if (chatSession.getMessages().size() > ChatConstant.SUMMARY_THRESHOLD) {
            chatSession.setSummary(summarizeOldMessages(chatSession.getMessages()));
        }

        return llmResponse;
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
        System.out.println("Similarity Search Data Size : "+documents.size());

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

}
