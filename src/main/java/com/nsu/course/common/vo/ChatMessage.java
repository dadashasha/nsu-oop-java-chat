package com.nsu.course.common.vo;

import com.nsu.course.common.dto.MessageDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@AllArgsConstructor
@Getter
@ToString
public class ChatMessage {
    private final String userName;
    private final String roomName;
    private final String content;
    private final Date date;

    public static ChatMessage createFrom(MessageDto message, String roomName) {
        return new ChatMessage(message.getUserName(), roomName, message.getMessage(), message.getTime());
    }
}
