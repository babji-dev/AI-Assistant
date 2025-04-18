package com.AI_assistant.Controller;

import com.AI_assistant.Constants.ChatConstant;
import com.AI_assistant.Models.ChatMessage;
import com.AI_assistant.Models.ChatSessionState;
import com.AI_assistant.Utils.UserSuggestionsUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {


    private final UserSuggestionsUtil userSuggestionsUtil;

    public ChatController(UserSuggestionsUtil userSuggestionsUtil){
        this.userSuggestionsUtil = userSuggestionsUtil;
    }

    @GetMapping("")
    public String chatPage(HttpSession session, Model model){
        ChatSessionState chatSession = getOrInitChatSession(session);
        String optionSelected = (String) session.getAttribute("optionSelected");
        String isWelcomeMessagePresent = (String) session.getAttribute("isWelcomeMessagePresent");
        List<ChatMessage> messages = chatSession.getMessages();
        if (optionSelected == null) {
            if(!"true".equalsIgnoreCase(isWelcomeMessagePresent)){
                String automatedResponse = "I'm here to help you with below documents : <br/> "+String.join("<br/>", userSuggestionsUtil.getAvailableOptions());
                messages.add(new ChatMessage("ai",automatedResponse, ChatConstant.AUTOMATED_MESSAGE_TYPE));
                session.setAttribute("isWelcomeMessagePresent","true");
            }
            model.addAttribute("messages", messages);
            return "chat";
        }
        model.addAttribute("messages", messages);
        return "chat";
    }

    @GetMapping("/clear")
    public String clearChat(HttpSession session, Model model){
        session.invalidate(); // or remove chat context
        return "redirect:/chat";
    }

    public static ChatSessionState getOrInitChatSession(HttpSession session) {
        ChatSessionState state = (ChatSessionState) session.getAttribute("chatSession");
        if (state == null) {
            state = new ChatSessionState();
            session.setAttribute("chatSession", state);
        }
        return state;
    }

}
