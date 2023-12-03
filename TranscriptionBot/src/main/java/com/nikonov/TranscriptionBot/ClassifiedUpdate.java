package com.nikonov.TranscriptionBot;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;

public class ClassifiedUpdate {
    @Getter
    @Setter
    private TelegramType telegramType; // enum, чтобы всё выглядило красиво

    @Getter
    @Setter
    private String messageText;

    @Getter
    private Message message;

    @Getter
    private final Update update; // сохраним сам update, чтобы в случае чего, его можно было достать

    @Getter
    @Setter
    private File file;

    @Getter
    private String userName; // @username

    @Getter
    private final Long userId;



    public ClassifiedUpdate(Update update) {
        this.update = update;
        this.telegramType = handleTelegramType();
        this.userId = handleUserId();
        this.messageText = handleMessageText();
    }


    private Long handleUserId() {
        if (telegramType == TelegramType.PreCheckoutQuery) {
            return update.getPreCheckoutQuery().getFrom().getId();
        } else if (telegramType == TelegramType.ChatJoinRequest) {
            userName = update.getChatJoinRequest().getUser().getUserName();
            return update.getChatJoinRequest().getUser().getId();
        } else if (telegramType == TelegramType.CallBack) {
            userName = update.getCallbackQuery().getFrom().getUserName();
            return update.getCallbackQuery().getFrom().getId();
        } else if (telegramType == TelegramType.MyChatMember) {
            userName = update.getMyChatMember().getChat().getUserName();
            return update.getMyChatMember().getFrom().getId();
        } else {
            userName = update.getMessage().getFrom().getUserName();
            return update.getMessage().getFrom().getId();
        }
    }

    //Обработаем команду.
    public String handleMessageText() {
        if (update.hasMessage()) {
            this.message = update.getMessage();
            if (update.getMessage().hasText()) {
                return update.getMessage().getText();
            }
        } else if (update.hasCallbackQuery()) {
            this.message = update.getCallbackQuery().getMessage();
            return update.getCallbackQuery().getData();
        }
        return "";
    }

    //Обработаем тип сообщения
    private TelegramType handleTelegramType() {
        if (update.hasCallbackQuery())
            return TelegramType.CallBack;

        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                if (update.getMessage().getText().startsWith("/"))
                    return TelegramType.Command;
                else
                    return TelegramType.Text;
            } else if (update.getMessage().hasSuccessfulPayment()) {
                return TelegramType.SuccessPayment;
            } else if (update.getMessage().hasPhoto())
                return TelegramType.Photo;
        } else if (update.hasPreCheckoutQuery()) {
            return TelegramType.PreCheckoutQuery;
        } else if (update.hasChatJoinRequest()) {
            return TelegramType.ChatJoinRequest;
        } else if (update.hasChannelPost()) {
            return TelegramType.ChannelPost;
        } else if (update.hasMyChatMember()) {
            return TelegramType.MyChatMember;
        }
        if (update.getMessage().hasDocument()) {
            return TelegramType.Text;
        }
        return TelegramType.Unknown;
    }

}