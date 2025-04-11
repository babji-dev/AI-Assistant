package com.AI_assistant.service;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    private final VectorStore vectorStore;

    private final JdbcClient jdbcClient;

    @Value("classpath:/Automated_Ticket_Booking_Tool.pdf")
    private Resource pdfResource;

    @Autowired
    public DataLoader(VectorStore vectorStore,JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }


    @PostConstruct
    public void init(){
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
            vectorStore.accept(textSplitter.apply(reader.get()));

            System.out.println("Application is ready to start");
        }
    }


}
