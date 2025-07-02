package com.nsu.course.client.messages;

import com.nsu.course.common.vo.ChatMessage;
import com.nsu.course.common.dto.ChatDto;
import com.nsu.course.common.dto.MessageDto;

import java.util.Date;
import java.util.concurrent.Flow.*;
import java.util.concurrent.SubmissionPublisher;

public class MessagePublisher {
    private final SubmissionPublisher<ChatMessage> publisher;

    public MessagePublisher() {
        this.publisher = new SubmissionPublisher<>();
    }

    public void addSubscriber(Subscriber<ChatMessage> subscriber) {
        publisher.subscribe(subscriber);
    }

    public void publish(ChatDto chat, String roomName) {
        for (MessageDto message : chat.getMessages()) {
            publisher.submit(ChatMessage.createFrom(message, roomName));
        }
    }

    public void publish(MessageDto message, String roomName) {
        publisher.submit(ChatMessage.createFrom(message, roomName));
    }

    public void publishRoomCreation(String userName, String roomName) {
        MessageDto dto = new MessageDto(userName, "Создал новую комнату", new Date());
        publisher.submit(ChatMessage.createFrom(dto, roomName));
    }
}