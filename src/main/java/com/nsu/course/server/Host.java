package com.nsu.course.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.server.Server;
import org.hibernate.HibernateException;

import java.util.Scanner;

public class Host {
    private static final Logger LOGGER = LogManager.getLogger(Host.class);
    private static final int PORT = 8080;
    private static final String IP = "localhost";
    private static final String DB_URL = "jdbc:sqlite:chat_storage.db";

    private Server websocketServer;

    public static void main(String[] args) {
        new Host().start();
    }

    public void start() {
        initializeWebSocketServer();

        LOGGER.info("Сервер запущен и готов принимать подключения");
        new Thread(this::handleConsoleInput).start();
    }

    private void initializeWebSocketServer() {
        websocketServer = new Server(IP, PORT, "/", null, ChatConnection.class);
        try {
            websocketServer.start();
        } catch (Exception e) {
            LOGGER.error("Ошибка запуска WebSocket сервера: {}", e.getMessage());
        }
    }

    private void handleConsoleInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            if ("stop".equalsIgnoreCase(command)) {
                shutdown();
                break;
            }
        }
        scanner.close();
    }

    public void shutdown() {
        LOGGER.info("Остановка сервиса...");
        try {
            websocketServer.stop();
            ChatRepository.closeSessionFactory();
        } catch (HibernateException e) {
            LOGGER.error("Ошибка при закрытии соединения с базой данных: {}", e.getMessage());
            System.exit(1);
        }
        LOGGER.info("Сервис остановлен...");
    }
}