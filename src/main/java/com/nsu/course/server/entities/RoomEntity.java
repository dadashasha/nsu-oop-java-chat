package com.nsu.course.server.entities;

import com.nsu.course.common.vo.ChatRoom;
import com.nsu.course.common.vo.ChatUser;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Getter
@Entity
@Table(name = "rooms")
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserEntity> roomUsers;

    public static RoomEntity fromChatRoom(ChatRoom room) {
        var newRoom = new RoomEntity();
        newRoom.name = room.getName();
        newRoom.roomUsers = List.of();
        return newRoom;
    }

    public boolean containsUser(ChatUser user) {
        return user.getRoomName().equals(name) && this.roomUsers.stream().anyMatch(
                userEntity -> userEntity.getName().equals(user.getName()));
    }

    public void removeUser(ChatUser user) {
        this.roomUsers.removeIf(userEntity -> userEntity.getName().equals(user.getName()));
    }
}
