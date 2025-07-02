package com.nsu.course;

import com.nsu.course.client.App;
import com.nsu.course.client.messages.MessagePublisher;
import com.nsu.course.server.Host;
import com.nsu.course.utils.TestMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

public class AppTest {
    private static final String FRIEND_NAME = "bober";
    private static final String MY_USER_NAME = "pivo";
    private static final String MY_ROOM_NAME = "419m";

    private Host host;

    @BeforeEach
    public void setup() {
        this.host = new Host();
        this.host.start();
    }

    @AfterEach
    public void tearDown() {
        host.shutdown();
    }

    @Test
    public void enterEmptyRoom() throws ExecutionException, InterruptedException {
        TestMessageHandler messageHandler = new TestMessageHandler();
        MessagePublisher messagePublisher = new MessagePublisher();
        messagePublisher.addSubscriber(messageHandler);
        var app = new App();
        app.start(MY_USER_NAME, MY_ROOM_NAME, messagePublisher);

        messageHandler.awaitMessageCount(1).get();
        Assertions.assertTrue(messageHandler.getMessages().getFirst().getContent().contains("Создал новую комнату"));
    }

    @Test
    public void enterEmptyRoomAndWriteMessage() throws ExecutionException, InterruptedException {
        TestMessageHandler messageHandler = new TestMessageHandler();
        MessagePublisher messagePublisher = new MessagePublisher();
        messagePublisher.addSubscriber(messageHandler);
        var app = new App();
        app.start(MY_USER_NAME, MY_ROOM_NAME, messagePublisher);

        messageHandler.awaitMessageCount(1).get();
        Assertions.assertTrue(messageHandler.getMessages().getFirst().getContent().contains("Создал новую комнату"));

        app.consumeInput("Hihi haha").get();
        messageHandler.awaitMessageCount(1).get();
        Assertions.assertTrue(messageHandler.getMessages().get(1).getContent().contains(MY_USER_NAME));
    }

    @Test
    public void enterReadyRoomAndFetchMessages() throws ExecutionException, InterruptedException {
        TestMessageHandler messageHandler = new TestMessageHandler();
        MessagePublisher messagePublisher = new MessagePublisher();
        messagePublisher.addSubscriber(messageHandler);
        var app = new App();
        app.start(MY_USER_NAME, MY_ROOM_NAME, messagePublisher);

        messageHandler.awaitMessageCount(1).get();
        Assertions.assertTrue(messageHandler.getMessages().getFirst().getContent().contains("Создал новую комнату"));

        app.consumeInput("Hihi haha").get();
        app.consumeInput("Haha hihi").get();
        messageHandler.awaitMessageCount(2).get();

        TestMessageHandler friendMessageHandler = new TestMessageHandler();
        MessagePublisher friendMessagePublisher = new MessagePublisher();
        friendMessagePublisher.addSubscriber(friendMessageHandler);
        var friendApp = new App();
        friendApp.start(FRIEND_NAME, MY_ROOM_NAME, friendMessagePublisher);
        friendMessageHandler.awaitMessageCount(2).get();
    }

    @Test
    public void enterReadyRoomAndIExitFromIt() throws ExecutionException, InterruptedException {
        TestMessageHandler messageHandler = new TestMessageHandler();
        MessagePublisher messagePublisher = new MessagePublisher();
        messagePublisher.addSubscriber(messageHandler);
        var app = new App();
        app.start(MY_USER_NAME, MY_ROOM_NAME, messagePublisher);

        messageHandler.awaitMessageCount(1).get();
        Assertions.assertTrue(messageHandler.getMessages().getFirst().getContent().contains("Создал новую комнату"));

        app.consumeInput("Hihi haha").get();
        app.consumeInput("Haha hihi").get();
        messageHandler.awaitMessageCount(2).get();

        TestMessageHandler friendMessageHandler = new TestMessageHandler();
        MessagePublisher friendMessagePublisher = new MessagePublisher();
        friendMessagePublisher.addSubscriber(friendMessageHandler);
        var friendApp = new App();
        friendApp.start(FRIEND_NAME, MY_ROOM_NAME, friendMessagePublisher);
        friendMessageHandler.awaitMessageCount(2).get();

        friendApp.consumeInput("bebebe").get();
        friendMessageHandler.awaitMessageCount(1).get();

        friendApp.consumeInput("exit").get();
    }
}
