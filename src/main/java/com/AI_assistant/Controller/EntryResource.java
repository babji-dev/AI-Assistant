package com.AI_assistant.Controller;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.directory.SearchResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class EntryResource {

    private final OllamaChatModel chatClient;

    private final VectorStore vectorStore;

    private String prompt = """
            As a subject matter expert you need to answer the requested question basis document section provided.
            If you're unsure about that you can mention that requested query not in your knowledge bank.
            
            QUESTION:
            {input}
            
            DOCUMENTS:
            {documents}
            
            """;

    @Autowired
    public EntryResource(OllamaChatModel chatClient,VectorStore vectorStore){
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/test/{message}")
    public String answer(@PathVariable String message){

        PromptTemplate template = new PromptTemplate(prompt);

        Map<String,Object> promptParameters = new HashMap<>();
        promptParameters.put("input",message);
        promptParameters.put("documents",findSimilarData(message));


        return chatClient.call(template.createMessage(promptParameters));
    }

    private String findSimilarData(String message) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().topK(5)
                .query(message)
                .build());

        return documents.stream().map(Document::getFormattedContent).collect(Collectors.joining());
    }

}
