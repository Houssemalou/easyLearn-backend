package com.free.easyLearn.repository;

import com.free.easyLearn.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByUserId(UUID userId);

    Optional<Student> findByUniqueCode(String uniqueCode);

    @Query("SELECT s FROM Student s ORDER BY s.user.name ASC")
    Page<Student> findAllStudents(Pageable pageable);

    // Stats queries
    @Query("SELECT s.level, COUNT(s) FROM Student s GROUP BY s.level ORDER BY s.level")
    java.util.List<Object[]> countByLevel();

    @Query("SELECT s FROM Student s ORDER BY s.createdAt DESC")
    java.util.List<Student> findRecentStudents(Pageable pageable);
}
