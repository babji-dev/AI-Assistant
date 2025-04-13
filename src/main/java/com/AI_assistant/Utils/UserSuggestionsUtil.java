package com.AI_assistant.Utils;

import com.AI_assistant.Constants.ChatConstant;
import com.AI_assistant.Models.ValidOptionDto;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserSuggestionsUtil {

    private final JdbcClient jdbcClient;

    private final List<String> availableOptions = new ArrayList<>();

    private final List<String> fileNames = new ArrayList<>();

    private final Map<String,String> fileVsDescMap = new LinkedHashMap<>();

    public UserSuggestionsUtil(JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
    }

    public String getWelcomeMessageWithOptions(){

        return "";
    }

    @PostConstruct
    public void loadOptionsFromDbToList(){

        Map<String, String> resultV2 = jdbcClient
                .sql("SELECT filename, description FROM loaded_files")
                .query((rs, rowNum) -> Map.entry(
                        rs.getString("filename"),
                        rs.getString("description")
                ))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,  (existing, replacement) -> replacement, LinkedHashMap::new));

        fileVsDescMap.putAll(resultV2);
        fileNames.addAll(resultV2.keySet());
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            availableOptions.add((i + 1) + " . " + resultV2.get(fileName));
        }
    }

    public List<String> getAvailableOptions() {
        return availableOptions;
    }

    public ValidOptionDto isValidOption(String userInput){
        ValidOptionDto isValid = new ValidOptionDto();
        if(ChatConstant.JUNIT.equalsIgnoreCase(userInput)){
            isValid.setValid(true);
            isValid.setOptionType(ChatConstant.OPTION_TYPE_JUNIT);
            isValid.setSource(ChatConstant.OPTION_TYPE_JUNIT);
            return isValid;
        }
        try{
            int choice = Integer.parseInt(userInput);
            if(choice>=1 && choice<=fileNames.size()){
                isValid.setValid(true);
                isValid.setOptionType(ChatConstant.OPTION_TYPE_DOCUMENT);
                isValid.setSource(fileNames.get(choice-1));
            }
        }catch(Exception e){
            System.out.println("Exception while validating the input across available options :: "+e.getMessage());
            return isValid;
        }
        System.out.println(isValid);
        return isValid;
    }
}
