package com.AI_assistant.service;

import jakarta.annotation.PostConstruct;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader {

    private final VectorStore vectorStore;

    private final JdbcClient jdbcClient;

    @Value("${documents.path}")
    private Resource[] pdfResources;

    @Autowired
    public DataLoader(VectorStore vectorStore, JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }


    @PostConstruct
    public void init() {

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build();
        var textSplitter = new TokenTextSplitter();

        for (Resource resource : pdfResources) {
            String filename = resource.getFilename();
            boolean alreadyLoaded = jdbcClient.sql("SELECT COUNT(*) FROM loaded_files WHERE filename = ?").param(1, filename).query(Integer.class).single() > 0;
            if (alreadyLoaded) {
                System.out.println("Skipping already loaded: " + filename);
                continue;
            }
            try {
                List<Document> chunks = switch (getExtension(filename)) {
                    case "pdf" -> {
                        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, config);
                        yield textSplitter.apply(reader.get());
                    }
                    case "txt" -> {
                        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        yield textSplitter.apply(List.of(new Document(content)));
                    }
                    case "docx" -> {
                        try (InputStream is = resource.getInputStream()) {
                            XWPFDocument doc = new XWPFDocument(is);
                            StringBuilder sb = new StringBuilder();
                            for (XWPFParagraph para : doc.getParagraphs()) {
                                sb.append(para.getText()).append("\n");
                            }
                            String content = sb.toString();
                            yield textSplitter.apply(List.of(new Document(content)));
                        }
                    }
                    default -> {
                        System.out.println("Unsupported file type: " + filename);
                        yield List.of();
                    }
                };

                chunks.forEach(doc -> doc.getMetadata().put("source", filename));
                vectorStore.accept(chunks);

                // Mark as loaded
                jdbcClient.sql("INSERT INTO loaded_files (filename) VALUES (?)").param(1, filename).update();

                System.out.println("Loaded: " + filename);
            } catch (Exception e) {
                System.err.println("Failed to process: " + filename + " - " + e.getMessage());
            }
        }



/*
        Integer count  = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class).single();
        System.out.println("No of Records present "+count);
        if(count<=0){
            System.out.println("Loading Indian Constitution "+count);
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();
            PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource,config);
            var textSplitter = new TokenTextSplitter();
            List<Document> chunks = textSplitter.apply(reader.get());
            chunks.forEach(doc -> doc.getMetadata().put("source",pdfResource.getFilename()));
            vectorStore.accept(chunks);

            System.out.println("Application is ready to start");
        }
        */
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }


}
