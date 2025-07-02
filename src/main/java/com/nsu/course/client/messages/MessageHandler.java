package com.nsu.course.client.messages;

import com.nsu.course.common.vo.ChatMessage;

import java.util.concurrent.Flow;

public interface MessageHandler extends Flow.Subscriber<ChatMessage> {
}
