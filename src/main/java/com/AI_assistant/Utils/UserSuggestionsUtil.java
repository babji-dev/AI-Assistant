package com.AI_assistant.Utils;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserSuggestionsUtil {

    private final JdbcClient jdbcClient;

    private final List<String> availableOptions = new ArrayList<>();

    public UserSuggestionsUtil(JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
    }

    public String getWelcomeMessageWithOptions(){

        return "";
    }

    @PostConstruct
    public void loadOptionsFromDbToList(){
        List<String> results = jdbcClient
                .sql("SELECT filename FROM loaded_files")
                .query(String.class)
                .list();
        availableOptions.addAll(results);
    }

    public List<String> getAvailableOptions() {
        return availableOptions;
    }

    public boolean isValidOption(String userInput){

        try{
            if(availableOptions.contains(userInput)){
                return true;
            }
        }catch(Exception e){
            System.out.println("Exception while validating the input across available options :: "+e.getMessage());
            return false;
        }
        return false;
    }
}
