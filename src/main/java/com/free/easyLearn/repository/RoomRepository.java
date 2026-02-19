package com.free.easyLearn.repository;

import com.free.easyLearn.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByLivekitRoomName(String livekitRoomName);

    @Query("SELECT r FROM Room r ORDER BY r.scheduledAt DESC")
    Page<Room> findAllRooms(Pageable pageable);

    // Stats queries
    long countByStatus(Room.RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.status = :status ORDER BY r.scheduledAt ASC")
    List<Room> findByStatus(@Param("status") Room.RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.professor.id = :professorId AND r.status = :status ORDER BY r.scheduledAt ASC")
    List<Room> findByProfessorIdAndStatus(@Param("professorId") UUID professorId, @Param("status") Room.RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.professor.id = :professorId")
    List<Room> findAllByProfessorId(@Param("professorId") UUID professorId);

    @Query("SELECT DISTINCT r FROM Room r JOIN r.participants p WHERE p.student.id = :studentId AND p.invited = true AND r.status = :status ORDER BY r.scheduledAt ASC")
    List<Room> findByStudentIdAndStatus(@Param("studentId") UUID studentId, @Param("status") Room.RoomStatus status);

    @Query("SELECT DISTINCT r FROM Room r JOIN r.participants p WHERE p.student.id = :studentId AND p.invited = true")
    List<Room> findAllByStudentId(@Param("studentId") UUID studentId);

    // Get rooms where a professor is assigned
    @Query("SELECT r FROM Room r WHERE r.professor.id = :professorId ORDER BY r.scheduledAt DESC")
    Page<Room> findByProfessorId(@Param("professorId") UUID professorId, Pageable pageable);

    // Get rooms where a student is invited
    @Query("SELECT DISTINCT r FROM Room r " +
            "JOIN r.participants p " +
            "WHERE p.student.id = :studentId AND p.invited = true " +
            "ORDER BY r.scheduledAt DESC")
    Page<Room> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);
}
