package com.nikonov.TranscriptionBot;


import ai.rev.speechtotext.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@EnableScheduling
@EnableAsync
public class Config {

    public final TranscriptionBot transcriptionBot;

    public Config(TranscriptionBot transcriptionBot) {
        this.transcriptionBot = transcriptionBot;
    }

    @Bean
    public TelegramBotsApi bot() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(transcriptionBot);
        return botsApi;
    }

}

