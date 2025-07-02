package com.nsu.course.client;

import com.nsu.course.client.messages.ConsoleMessageHandler;
import com.nsu.course.client.messages.MessageHandler;
import com.nsu.course.client.messages.MessagePublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private static final int PORT = 8080;
    private static final String SERVER_IP = "localhost";
    private static final String SERVER_URI_TEMPLATE = "ws://%s:%d/chat";
    private static final String SERVER_URI = String.format(SERVER_URI_TEMPLATE, SERVER_IP, PORT);

    private ClientConnection connection;

    public static void main(String[] args) {
        if (args.length != 2) {
            LOGGER.error("Неправильный ввод, введите: java App [имя_пользователя] [имя_комнаты]");
            System.exit(1);
        }
        String userName = args[0].trim();
        String roomName = args[1].trim();

        ConsoleMessageHandler messageHandler = new ConsoleMessageHandler();
        MessagePublisher messagePublisher = new MessagePublisher();
        messagePublisher.addSubscriber(messageHandler);

        var app = new App();
        app.start(userName, roomName, messagePublisher);
        app.doChatLoop(roomName);
    }

    public Future<?> consumeInput(String input) {
        if (input.equalsIgnoreCase("exit")) {
            connection.close();
            return CompletableFuture.completedFuture(null);
        } else if (input.equalsIgnoreCase("")) {
            return CompletableFuture.completedFuture(null);
        } else {
            return connection.sendMessage(input);
        }
    }

    public void start(String userName, String roomName, MessagePublisher messagePublisher) {
        if (connection != null) {
            throw new IllegalStateException("Already launched");
        }

        LOGGER.info("Связываемся с сервером: {}", SERVER_URI);
        this.connection = ClientConnection.setup(SERVER_URI, userName, roomName, messagePublisher);
    }

    private void doChatLoop(String roomName) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Для выхода из чата введите exit\n");
            while (true) {
                System.out.println(":");
                String input = scanner.nextLine();
                consumeInput(input).get();
            }
        } catch (Exception e) {
            LOGGER.error("Возникло исключение: {}", e.getMessage());
        } finally {
            LOGGER.info("Завершение работы");
        }
    }
}