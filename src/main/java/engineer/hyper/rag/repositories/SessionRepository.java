package engineer.hyper.rag.repositories;


import engineer.hyper.rag.entities.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    List<SessionEntity> findByAppNameAndUserId(String appName, String userId);

    @Query("SELECT s FROM SessionEntity s WHERE s.id = :sessionId AND s.appName = :appName AND s.userId = :userId")
    SessionEntity findBySessionIdAndAppNameAndUserId(
            @Param("sessionId") String sessionId,
            @Param("appName") String appName,
            @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM SessionEntity s WHERE s.appName = :appName AND s.userId = :userId AND s.id = :sessionId")
    int deleteByAppNameAndUserIdAndId(@Param("appName") String appName,
                                      @Param("userId") String userId,
                                      @Param("sessionId") String sessionId);

    boolean existsByIdAndAppNameAndUserId(String id, String appName, String userId);

    // In SessionRepository.java
    @Modifying
    @Query("DELETE FROM EventEntity e WHERE e.session.id = :sessionId")
    void deleteEventsBySessionId(@Param("sessionId") String sessionId);

    // In SessionRepository.java - DELETE operation
    @Modifying
    @Query("DELETE FROM SessionEntity s WHERE s.id = :sessionId AND s.appName = :appName AND s.userId = :userId")
    int deleteSessionIfExists(@Param("sessionId") String sessionId,
                              @Param("appName") String appName,
                              @Param("userId") String userId);
}