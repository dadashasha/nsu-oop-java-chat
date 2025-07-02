package com.nsu.course.server.entities;

import com.nsu.course.common.vo.ChatUser;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Getter
@Entity()
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    private List<MessageEntity> messages;

    public static UserEntity fromChatUser(ChatUser user) {
        UserEntity userEntity = new UserEntity();
        userEntity.name = user.getName();
        return userEntity;
    }
}
