package com.AI_assistant.Controller;

import com.AI_assistant.Models.ChatMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @GetMapping("")
    public String chatPage(HttpSession session, Model model){
        List<ChatMessage> messages = getMessagesFromSession(session);
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

}
