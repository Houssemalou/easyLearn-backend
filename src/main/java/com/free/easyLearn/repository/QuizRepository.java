//package com.free.easyLearn.repository;
//
//import com.lingua.hub.entity.Quiz;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.UUID;
//
//@Repository
//public interface QuizRepository extends JpaRepository<Quiz, UUID> {
//
//    List<Quiz> findBySessionId(UUID sessionId);
//
//    @Query("SELECT q FROM Quiz q WHERE " +
//           "(:sessionId IS NULL OR q.session.id = :sessionId) AND " +
//           "(:language IS NULL OR q.language = :language) AND " +
//           "(:level IS NULL OR q.level = :level) AND " +
//           "(:isPublished IS NULL OR q.isPublished = :isPublished) AND " +
//           "(:createdBy IS NULL OR q.createdBy.id = :createdBy) AND " +
//           "(:search IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//           "LOWER(q.description) LIKE LOWER(CONCAT('%', :search, '%')))")
//    Page<Quiz> findByFilters(
//            @Param("sessionId") UUID sessionId,
//            @Param("language") String language,
//            @Param("level") com.lingua.hub.entity.Student.LanguageLevel level,
//            @Param("isPublished") Boolean isPublished,
//            @Param("createdBy") UUID createdBy,
//            @Param("search") String search,
//            Pageable pageable
//    );
//}
