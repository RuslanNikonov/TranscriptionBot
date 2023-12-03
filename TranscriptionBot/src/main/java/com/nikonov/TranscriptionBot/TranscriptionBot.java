package com.nikonov.TranscriptionBot;


import ai.rev.speechtotext.ApiClient;
import ai.rev.speechtotext.models.asynchronous.RevAiJob;
import ai.rev.speechtotext.models.asynchronous.RevAiJobOptions;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class TranscriptionBot extends TelegramLongPollingCommandBot {

    private final ApiClient apiClient;

    private final String botToken;

    public TranscriptionBot(@Value("${ai.token}") String aiToken,
                            @Value("${bot.token}") String botToken) {
        super();
        apiClient = new ApiClient(aiToken);
        this.botToken = botToken;
    }



    @Override
    public String getBotUsername() {
        return "bot";
    }


    @Override
    public String getBotToken() {
        return botToken;
    }



    @SneakyThrows
    @Override
    public void processNonCommandUpdate(Update update) {
        //Создаем эксземпляр вспомогательного класса.
        ClassifiedUpdate classifiedUpdate = new ClassifiedUpdate(update);

        //Получаем путь к полученному аудио или видео
        String localPathToFile = checkAndDownload(classifiedUpdate);


        if (localPathToFile != null) { // Если файл есть в сообщении
            // Создаем опции и эксзепляр класса, который отвечает за связь с сервисом https://www.rev.ai/
            RevAiJobOptions revAiJobOptions = new RevAiJobOptions();
            revAiJobOptions.setSkipPunctuation(false);
            revAiJobOptions.setSkipDiarization(false);
            revAiJobOptions.setSkipPostprocessing(false);
            revAiJobOptions.setLanguage("ru");

            // Отправляем наш файл на сервис
            RevAiJob revAiJob = apiClient.submitJobLocalFile(localPathToFile, revAiJobOptions);


            // Проверяем статус задания
            RevAiJob newlyRefreshedRevAiJob = apiClient.getJobDetails(revAiJob.getJobId());
            while (newlyRefreshedRevAiJob.getJobStatus().getStatus().equals("in_progress")) { // Пока расшифровка не закончилась
                Thread.sleep(1000); // ждем секунду
                // запрашиваем статус задания
                newlyRefreshedRevAiJob = apiClient.getJobDetails(revAiJob.getJobId());
            }


            // Вытаскиваем полученный текст
            String transcriptText = apiClient.getTranscriptText(revAiJob.getJobId());

            // убираем спикера и тайминг
            transcriptText = transcriptText.replaceAll("Speaker\\s+\\d+\\s+\\d{2}:\\d{2}:\\d{2}\\s+", "");

            // Отправляем сообщение с расшифровкой
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(classifiedUpdate.getMessage().getChatId());
            sendMessage.setText(transcriptText);
            execute(sendMessage);

            // Удаляем временный файл
            java.io.File file = new java.io.File(localPathToFile);
            file.delete();
        }
    }

    @SneakyThrows
    private String checkAndDownload(ClassifiedUpdate update) {
        //Проверяем последовательно имеется ли в сообщении голос, аудио или видео и скачиваем этот файл
        if (update.getMessage().hasVoice()) {
            System.out.println("detect!");
            Message message = update.getMessage();
            Voice voice = message.getVoice();
            java.io.File javaFile = getFile(voice.getFileId());

            return javaFile.getPath();

        }
        if (update.getMessage().hasVideoNote()) {
            System.out.println("detect!");
            Message message = update.getMessage();
            VideoNote videoNote = message.getVideoNote();
            java.io.File javaFile = getFile(videoNote.getFileId());

            return javaFile.getPath();

        }

        if (update.getMessage().hasAudio()) {
            System.out.println("detect!");
            Message message = update.getMessage();
            Audio audio = message.getAudio();
            java.io.File javaFile = getFile(audio.getFileId());
            return javaFile.getPath();
        }

        if (update.getMessage().hasVideo()) {
            System.out.println("detect!");
            Message message = update.getMessage();
            Video video = message.getVideo();
            java.io.File javaFile = getFile(video.getFileId());

            return  javaFile.getPath();
        }
        return null;
    }


    @SneakyThrows
    private java.io.File getFile(String fileId) { // Скачиваем файл на компьютер как временный файл
        GetFile getFile = new GetFile(fileId);
        File file = this.execute(getFile);
        java.io.File javaFile = java.io.File.createTempFile("tlgbotfile" + fileId, ".tmp");
        this.downloadFile(file, javaFile);
        return javaFile;
    }


}
