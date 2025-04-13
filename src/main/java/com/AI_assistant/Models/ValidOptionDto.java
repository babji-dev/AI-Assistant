package com.AI_assistant.Models;

public class ValidOptionDto {

    private boolean valid;
    private String source;
    private String optionType;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    @Override
    public String toString() {
        return "ValidOptionDto{" +
                "valid=" + valid +
                ", source='" + source + '\'' +
                ", optionType='" + optionType + '\'' +
                '}';
    }
}
