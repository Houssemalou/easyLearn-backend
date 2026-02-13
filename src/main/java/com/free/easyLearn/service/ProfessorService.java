package com.free.easyLearn.service;

import com.free.easyLearn.dto.professor.CreateProfessorRequest;
import com.free.easyLearn.dto.professor.ProfessorDTO;
import com.free.easyLearn.dto.professor.UpdateProfessorRequest;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProfessorService {

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public ProfessorDTO createProfessor(CreateProfessorRequest request) {
        // Check if user with email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.PROFESSOR)
                .avatar(null)
                .build();
        user = userRepository.save(user);

        // Create professor
        Professor professor = Professor.builder()
                .user(user)
                .languages(request.getLanguages())
                .specialization(request.getSpecialization())
                .bio(request.getBio())
                .rating(BigDecimal.ZERO)
                .totalSessions(0)
                .build();

        professor = professorRepository.save(professor);
        return mapToDTO(professor);
    }

    @Transactional
    public ProfessorDTO updateProfessor(UUID professorId, UpdateProfessorRequest request) {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        User user = professor.getUser();
        
        if (request.getName() != null) user.setName(request.getName());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getLanguages() != null) professor.setLanguages(request.getLanguages());
        if (request.getSpecialization() != null) professor.setSpecialization(request.getSpecialization());
        if (request.getBio() != null) professor.setBio(request.getBio());

        userRepository.save(user);
        professor = professorRepository.save(professor);
        return mapToDTO(professor);
    }

    @Transactional
    public void deleteProfessor(UUID professorId) {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
        
        User user = professor.getUser();
        professorRepository.delete(professor);
        userRepository.delete(user);
    }

    public ProfessorDTO getProfessorById(UUID professorId) {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
        return mapToDTO(professor);
    }

public Page<ProfessorDTO> getProfessors(int page, int size, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Professor> professors = professorRepository.findAllProfessors(pageable);

        return professors.map(this::mapToDTO);
    }

    private ProfessorDTO mapToDTO(Professor professor) {
        return ProfessorDTO.builder()
                .id(professor.getId())
                .name(professor.getUser() != null ? professor.getUser().getName() : null)
                .email(professor.getUser() != null ? professor.getUser().getEmail() : null)
                .avatar(professor.getUser() != null ? professor.getUser().getAvatar() : null)
                .languages(professor.getLanguages())
                .specialization(professor.getSpecialization())
                .bio(professor.getBio())
                .rating(professor.getRating())
                .totalSessions(professor.getTotalSessions())
                .joinedAt(professor.getCreatedAt())
                .build();
    }
}
