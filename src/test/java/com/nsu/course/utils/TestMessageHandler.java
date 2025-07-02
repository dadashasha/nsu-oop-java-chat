package com.nsu.course.utils;

import com.nsu.course.client.messages.MessageHandler;
import com.nsu.course.common.vo.ChatMessage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMessageHandler implements MessageHandler {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private Flow.Subscription subscription;
    private final AtomicInteger counter = new AtomicInteger(0);

    @Getter
    private final ArrayList<ChatMessage> messages = new ArrayList<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(ChatMessage item) {
        this.messages.add(item);
        this.counter.incrementAndGet();
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onComplete() {
    }

    public CompletableFuture<Void> awaitMessageCount(int count) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        EXECUTOR.submit(() -> {
            while (true) {
                if (this.counter.compareAndSet(count, 0)) {
                    future.complete(null);
                    break;
                }
            }
        });
        return future;
    }
}
