package com.free.easyLearn.service;

import com.free.easyLearn.dto.student.CreateStudentRequest;
import com.free.easyLearn.dto.student.StudentDTO;
import com.free.easyLearn.dto.student.StudentSkillsDTO;
import com.free.easyLearn.dto.student.UpdateStudentRequest;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.StudentSkills;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public StudentDTO createStudent(CreateStudentRequest request) {
        // Check if user with email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.STUDENT)
                .avatar(null)
                .build();
        user = userRepository.save(user);

        // Create student
        Student student = Student.builder()
                .user(user)
                .nickname(request.getNickname())
                .level(request.getLevel())
                .bio(request.getBio())
                .build();

        student = studentRepository.save(student);

        return mapToDTO(student);
    }

    @Transactional
    public StudentDTO updateStudent(UUID studentId, UpdateStudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        User user = student.getUser();

        if (request.getName() != null) user.setName(request.getName());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getLevel() != null) student.setLevel(request.getLevel());
        if (request.getNickname() != null) student.setNickname(request.getNickname());
        if (request.getBio() != null) student.setBio(request.getBio());

        userRepository.save(user);
        student = studentRepository.save(student);
        return mapToDTO(student);
    }

    @Transactional
    public void deleteStudent(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        User user = student.getUser();
        studentRepository.delete(student);
        userRepository.delete(user);
    }

    public StudentDTO getStudentById(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return mapToDTO(student);
    }

    public StudentDTO getStudentByUserId(UUID userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for this user"));
        return mapToDTO(student);
    }

    public Page<StudentDTO> getStudents(int page, int size, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Student> students = studentRepository.findAllStudents(pageable);

        return students.map(this::mapToDTO);
    }

    public Page<StudentDTO> getStudentsByAdmin(UUID adminId, int page, int size, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Student> students = studentRepository.findAllStudentsByAdmin(adminId, pageable);

        return students.map(this::mapToDTO);
    }

    public List<StudentDTO> getStudentsByIds(List<UUID> studentIds) {
        List<Student> students = studentRepository.findAllById(studentIds);
        return students.stream()
                .map(this::mapToDTO)
                .toList();
    }

    private StudentDTO mapToDTO(Student student) {
        StudentSkills skillsEntity = student.getSkills();
        StudentSkillsDTO skillsDto = null;
        if (skillsEntity != null) {
            skillsDto = StudentSkillsDTO.builder()
                    .pronunciation(skillsEntity.getPronunciation())
                    .grammar(skillsEntity.getGrammar())
                    .vocabulary(skillsEntity.getVocabulary())
                    .fluency(skillsEntity.getFluency())
                    .build();
        }

        return StudentDTO.builder()
                .id(student.getId())
                .name(student.getUser() != null ? student.getUser().getName() : null)
                .email(student.getUser() != null ? student.getUser().getEmail() : null)
                .avatar(student.getUser() != null ? student.getUser().getAvatar() : null)
                .nickname(student.getNickname())
                .bio(student.getBio())
                .level(student.getLevel())
                .joinedAt(student.getJoinedAt())
                .totalSessions(student.getTotalSessions())
                .hoursLearned(student.getHoursLearned())
                .skills(skillsDto)
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
