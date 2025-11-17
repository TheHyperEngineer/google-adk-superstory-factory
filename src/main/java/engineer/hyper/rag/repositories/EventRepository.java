package engineer.hyper.rag.repositories;


import engineer.hyper.rag.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity, String> {

    @Query("SELECT e FROM EventEntity e WHERE e.session.id = :sessionId ORDER BY e.timestamp ASC")
    List<EventEntity> findBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EventEntity e WHERE e.session.id = :sessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);
}