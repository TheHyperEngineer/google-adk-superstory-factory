package engineer.hyper.rag.repositories;


import engineer.hyper.rag.entities.MemoryEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemoryEntryRepository extends JpaRepository<MemoryEntryEntity, String> {
    List<MemoryEntryEntity> findByAppNameAndUserId(String appName, String userId);

    // Simple case-insensitive search using LIKE
    @Query("SELECT m FROM MemoryEntryEntity m WHERE m.appName = :appName AND m.userId = :userId AND LOWER(m.searchText) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.timestamp DESC")
    List<MemoryEntryEntity> searchByAppNameAndUserIdAndQuery(@Param("appName") String appName,
                                                             @Param("userId") String userId,
                                                             @Param("query") String query);

    @Query("DELETE FROM MemoryEntryEntity m WHERE m.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);
}