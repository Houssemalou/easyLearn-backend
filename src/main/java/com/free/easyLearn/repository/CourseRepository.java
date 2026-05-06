package com.free.easyLearn.repository;

import com.free.easyLearn.entity.Course;
import com.free.easyLearn.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findAllByProfessorIdOrderByCreatedAtDesc(UUID professorId);
    List<Course> findAllByLevelOrderByCreatedAtDesc(Student.LanguageLevel level);
    Optional<Course> findByIdAndProfessorId(UUID id, UUID professorId);
}
