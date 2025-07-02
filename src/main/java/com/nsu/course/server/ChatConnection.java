package com.nsu.course.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.nsu.course.common.vo.ChatMessage;
import com.nsu.course.common.vo.ChatRoom;
import com.nsu.course.common.vo.ChatUser;
import com.nsu.course.common.dto.ChatDto;
import com.nsu.course.common.dto.MessageDto;
import com.nsu.course.common.dto.TransferCodes;
import com.nsu.course.server.entities.RoomEntity;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.utils.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ServerEndpoint(value = "/chat")
public class ChatConnection {
    private static final Logger LOGGER = LogManager.getLogger(ChatConnection.class);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private Map<String, Object> properties;
    private String userName;
    private String roomName;
    private boolean authorized;

    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("Новое подключение: {}", session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        EXECUTOR.submit(() -> processMessage(session, message));
    }

    private void processMessage(Session session, String message) {
        if (!authorized) {
            handleUserRegistration(session, message);
        } else {
            handleChatMessage(session, message);
        }
    }

    private void handleUserRegistration(Session session, String credentials) {
        try {
            String[] parts = credentials.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Неверные данные для входа");
            }
            String userName = parts[0].trim();
            String roomName = parts[1].trim();

            RoomEntity roomEntity = ChatRepository.getRoom(roomName);
            if (roomEntity == null) {
                ChatRepository.addRoom(new ChatRoom(roomName));
                ChatRepository.addUserToRoom(new ChatUser(userName, roomName));
                session.getUserProperties().put("userName", userName);
                session.getUserProperties().put("roomName", roomName);
                this.userName = userName;
                this.roomName = roomName;
                this.authorized = true;
                session.getAsyncRemote().sendText(TransferCodes.NEW_ROOM_CREATED);
                return;
            }

            if (roomEntity.containsUser(new ChatUser(userName, roomName))) {
                LOGGER.warn("Пользователь с таким именем уже есть в комнате: {}", userName);
                session.getAsyncRemote().sendText(TransferCodes.NAME_DUPLICATE_ERROR);
                return;
            }

            ChatRepository.addUserToRoom(new ChatUser(userName, roomName));
            session.getUserProperties().put("userName", userName);
            session.getUserProperties().put("roomName", roomName);
            this.userName = userName;
            this.roomName = roomName;
            this.authorized = true;

            try {
                roomEntity = ChatRepository.getRoom(roomName);
                List<MessageDto> messages = new ArrayList<>();
                roomEntity.getRoomUsers().forEach(user -> messages.addAll(user.getMessages().stream()
                        .map(m -> new MessageDto(user.getName(), m.getTextContent(), m.getDate()))
                        .toList()));
                var dto = new ChatDto(messages);
                String upload = TransferCodes.FETCH_MESSAGES + new ObjectMapper().writeValueAsString(dto);
                session.getAsyncRemote().sendText(upload);
            } catch (IOException exception) {
                LOGGER.error("Ошибка сериализации сообщений чата: {}", exception.getMessage());
                gracefulShutdown(session);
            }
        } catch (Exception e) {
            LOGGER.warn("Ошибка регистрации: {}", e.getMessage());
            gracefulShutdown(session);
        }
    }

    private void handleChatMessage(Session session, String message) {
        try {
            Date currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
            var chatMessage = new ChatMessage(this.userName, this.roomName, message, currentTime);
            ChatRepository.saveMessage(chatMessage);
            MessageDto messageDto = new MessageDto(this.userName, message, currentTime);
            String upload = TransferCodes.NEW_MESSAGE + new ObjectMapper().writeValueAsString(messageDto);
            session.getOpenSessions().stream()
                    .filter(s -> s.isOpen()  &&
                            s.getUserProperties().get("roomName").equals(this.roomName))
                    .forEach(s -> s.getAsyncRemote().sendText(upload));
        } catch (IOException e) {
            LOGGER.error("Ошибка сериализации сообщения: {}", e.getMessage());
            gracefulShutdown(session);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        ChatRepository.removeUserFromRoom(new ChatUser(this.userName, this.roomName));
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LOGGER.error("Ошибка сервера: {}", error.getMessage());
        gracefulShutdown(session);
    }

    private void gracefulShutdown(Session session) {
        EXECUTOR.shutdown();
        try {
            CloseReason reason = new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, TransferCodes.INTERNAL_ERROR);
            session.close(reason);
        } catch (IOException e) {
            LOGGER.warn("Ошибка при закрытии сессии: {}", e.getMessage());
            System.exit(1);
        }
    }
}
