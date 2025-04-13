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
        List<ChatMessage> messages = chatSession.getMessages();
        if (optionSelected == null) {
            String automatedResponse = String.join("\n", userSuggestionsUtil.getAvailableOptions());
            messages.add(new ChatMessage("ai",automatedResponse, ChatConstant.AUTOMATED_MESSAGE_TYPE));
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



    public static List<ChatMessage> getMessagesFromSession(HttpSession session) {
        List<ChatMessage> messages = (List<ChatMessage>) session.getAttribute("messages");
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute("messages", messages);
        }
        return messages;
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
