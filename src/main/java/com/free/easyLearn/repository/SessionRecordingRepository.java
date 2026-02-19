//package com.free.easyLearn.repository;
//
//import com.lingua.hub.entity.SessionRecording;
//import com.lingua.hub.entity.SessionRecording.RecordingStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
/// **
// * Repository for SessionRecording entity
// */
//@Repository
//public interface SessionRecordingRepository extends JpaRepository<SessionRecording, Long> {
//
//    /**
//     * Find recording by LiveKit recording ID
//     */
//    Optional<SessionRecording> findByLivekitRecordingId(String livekitRecordingId);
//
//    /**
//     * Find all recordings for a specific room
//     */
//    List<SessionRecording> findByRoomIdOrderByStartedAtDesc(Long roomId);
//
//    /**
//     * Find all recordings for a specific room with pagination
//     */
//    Page<SessionRecording> findByRoomId(Long roomId, Pageable pageable);
//
//    /**
//     * Find recordings by status
//     */
//    List<SessionRecording> findByStatus(RecordingStatus status);
//
//    /**
//     * Find all completed recordings for rooms where student participated
//     */
//    @Query("SELECT sr FROM SessionRecording sr " +
//           "JOIN RoomParticipant rp ON rp.room.id = sr.room.id " +
//           "WHERE rp.student.id = :studentId " +
//           "AND sr.status = 'COMPLETED' " +
//           "ORDER BY sr.startedAt DESC")
//    List<SessionRecording> findCompletedRecordingsByStudentId(@Param("studentId") Long studentId);
//
//    /**
//     * Find all completed recordings (for admin)
//     */
//    Page<SessionRecording> findByStatusOrderByStartedAtDesc(RecordingStatus status, Pageable pageable);
//
//    /**
//     * Count recordings by status
//     */
//    Long countByStatus(RecordingStatus status);
//}
