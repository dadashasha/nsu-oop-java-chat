package com.nsu.course.server.entities;

import com.nsu.course.common.vo.ChatMessage;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Date;

@Getter
@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OrderBy
    private Date date;

    private String textContent;

    public static MessageEntity fromChatMessage(ChatMessage m) {
        var messageEntity = new MessageEntity();
        messageEntity.textContent = m.getContent();
        return messageEntity;
    }
}
