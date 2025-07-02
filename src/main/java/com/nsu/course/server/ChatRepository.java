package com.nsu.course.server;

import com.nsu.course.common.vo.ChatMessage;
import com.nsu.course.common.vo.ChatRoom;
import com.nsu.course.common.vo.ChatUser;
import com.nsu.course.server.entities.MessageEntity;
import com.nsu.course.server.entities.RoomEntity;
import com.nsu.course.server.entities.UserEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class ChatRepository {
    private static final Object lock = new Object();
    private static final Logger LOGGER = LogManager.getLogger(ChatRepository.class);
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .build();
        return new MetadataSources(registry).buildMetadata().buildSessionFactory();
    }

    public static void closeSessionFactory() throws HibernateException {
        sessionFactory.close();
    }

    public static void addRoom(ChatRoom room) {
        synchronized (lock) {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                session.persist(RoomEntity.fromChatRoom(room));
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
            }
        }
    }

    public static RoomEntity getRoom(String roomName) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<RoomEntity> criteria = builder.createQuery(RoomEntity.class);
            Root<RoomEntity> root = criteria.from(RoomEntity.class);
            criteria.select(criteria.from(RoomEntity.class))
                    .where(builder.equal(root.get("name"), roomName));
            return session.createQuery(criteria).getResultList().stream().findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void addUserToRoom(ChatUser user) {
        synchronized (lock) {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                RoomEntity room = getRoom(user.getRoomName());
                if (room == null) {
                    throw new IllegalArgumentException("Room does not exist");
                }
                room.getRoomUsers().add(UserEntity.fromChatUser(user));
                session.merge(room);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
            }
        }
    }

    public static void removeUserFromRoom(ChatUser user) {
        synchronized (lock) {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                RoomEntity room = getRoom(user.getRoomName());
                if (room == null) {
                    throw new IllegalArgumentException("Room does not exist");
                }
                if (!room.containsUser(user)) {
                    throw new IllegalArgumentException("User does not exist in this room");
                }

                room.removeUser(user);
                session.merge(room);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
            }
        }
    }

    public static void saveMessage(ChatMessage msg) {
        synchronized (lock) {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                RoomEntity room = getRoom(msg.getRoomName());
                if (room == null) {
                    throw new IllegalArgumentException("Room does not exist");
                }

                UserEntity user = room.getRoomUsers().stream().filter(
                                userEntity -> userEntity.getName().equals(msg.getUserName()))
                        .findFirst().orElse(null);
                if (user == null) {
                    throw new IllegalArgumentException("User does not exist");
                }

                user.getMessages().add(MessageEntity.fromChatMessage(msg));
                session.merge(user);
                transaction.commit();
            } catch (Exception e) {
                LOGGER.error("Не получилось сохранить сообщение в базу данных", e);
                if (transaction != null) {
                    transaction.rollback();
                }
            }
        }
    }
}

