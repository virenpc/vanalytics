package com.example.demo.services;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TelegramMessageService {

    @Value("${bot_token}")
    private String token;
    @Value("${group_id}")
    private Long groupId;

    public void sendMessage(String message) {
        TelegramBot bot = new TelegramBot(token);
        SendResponse response = bot.execute(new SendMessage(Math.negateExact(groupId), message));

    }

}
