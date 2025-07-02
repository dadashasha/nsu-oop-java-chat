package com.nsu.course.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsu.course.client.messages.MessagePublisher;
import com.nsu.course.common.dto.ChatDto;
import com.nsu.course.common.dto.MessageDto;
import com.nsu.course.common.dto.TransferCodes;
import jakarta.websocket.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ClientEndpoint
public class ClientConnection implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(ClientConnection.class);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private final String serverUri;
    private final String userName;
    private final String roomName;
    private final MessagePublisher publisher;
    private Session session;

    private ClientConnection(String serverUri, String username, String roomNumber, MessagePublisher publisher) {
        this.serverUri = serverUri;
        this.userName = username;
        this.roomName = roomNumber;
        this.publisher = publisher;
    }

    public static ClientConnection setup(String serverUri, String username,
                                         String roomNumber, MessagePublisher publisher) {
        ClientConnection connection = new ClientConnection(serverUri, username, roomNumber, publisher);

        try {
            connection.connect();
            connection.enterRoom();
        } catch (Exception error) {
            connection.close();
            LOGGER.error("Провалено подключение к сервису", error);
            return null;
        }

        return connection;
    }

    @OnMessage
    public void onMessage(String message) {
        LOGGER.info("Пришло новое сообщение с сервиса {}", message);
        try {
            if (message.startsWith(TransferCodes.FETCH_MESSAGES)) {
                String jsonData = message.substring(TransferCodes.FETCH_MESSAGES.length());
                ChatDto dto = new ObjectMapper().readValue(jsonData, ChatDto.class);
                this.publisher.publish(dto, this.roomName);
            } else if (message.startsWith(TransferCodes.NEW_MESSAGE)) {
                String jsonData = message.substring(TransferCodes.NEW_MESSAGE.length());
                MessageDto dto = new ObjectMapper().readValue(jsonData, MessageDto.class);
                this.publisher.publish(dto, this.roomName);
            } else if (message.startsWith(TransferCodes.NEW_ROOM_CREATED)) {
                this.publisher.publishRoomCreation(this.userName, this.roomName);
            }
        } catch (Exception error) {
            LOGGER.error("Ошибка при десериализации сообщения", error);
        }
    }

    @OnError
    public void onError(Throwable error) {
        LOGGER.error("Ошибка подключения: {}", error.getMessage());
    }

    public Future<?> sendMessage(String content) {
        if (isDisconnected()) {
            LOGGER.error("Сообщение не отправлено из-за неустановленного соединения.");
            return CompletableFuture.failedFuture(new ConnectException());
        }

        return EXECUTOR.submit(() -> {
            String formattedMessage = userName + " : " + roomName + " : " + content;
            try {
                session.getBasicRemote().sendText(formattedMessage);
            } catch (IOException e) {
                LOGGER.error("Сообщение не отправлено из-за проблем с соединением.");
            }
        });
    }

    @Override
    public void close() {
        closeSession();
    }

    private void closeSession() {
        if (isDisconnected()) {
            LOGGER.warn("Сессия уже закрыта.");
            return;
        }

        try {
            session.close();
        } catch (Exception e) {
            LOGGER.warn("Ошибка при закрытии соединения: {}", e.getMessage());
        }
    }

    private boolean isDisconnected() {
        return this.session == null || !this.session.isOpen();
    }

    private void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, new URI(serverUri));
            LOGGER.info("Попытка подключения к серверу: {}", serverUri);
        } catch (Exception e) {
            LOGGER.error("Ошибка подключения к серверу: {}", e.getMessage());
        }
    }

    private void enterRoom() {
        if (isDisconnected()) {
            LOGGER.error("Невозможно войти в комнату из-за неустановленного соединения.");
            return;
        }

        try {
            String credentials = String.format("%s : %s", this.userName, this.roomName);
            session.getBasicRemote().sendText(credentials);
        } catch (Exception e) {
            LOGGER.warn("Ошибка при регистрации: {}", e.getMessage());
        }
    }
}
