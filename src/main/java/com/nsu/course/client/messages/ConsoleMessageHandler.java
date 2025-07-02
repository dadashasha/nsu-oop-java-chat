package com.nsu.course.client.messages;

import com.nsu.course.common.vo.ChatMessage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.concurrent.*;

public class ConsoleMessageHandler implements MessageHandler {
    private Flow.Subscription subscription;

    @Getter
    private ArrayList<ChatMessage> messages;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(ChatMessage chatMessage) {
        messages.add(chatMessage);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(throwable.getMessage());
    }

    @Override
    public void onComplete() {
    }
}
