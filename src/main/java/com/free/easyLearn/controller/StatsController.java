package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.stats.AdminStatsDTO;
import com.free.easyLearn.dto.stats.ProfessorStatsDTO;
import com.free.easyLearn.dto.stats.StudentStatsDTO;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Dashboard statistics for all actors")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // Admin Stats
    // ==========================================

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard statistics",
            description = "Returns all statistics for the admin dashboard including counts, live rooms, recent students")
    public ResponseEntity<ApiResponse<AdminStatsDTO>> getAdminStats() {
        AdminStatsDTO stats = statsService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success("Admin stats retrieved", stats));
    }

    // ==========================================
    // Professor Stats
    // ==========================================

    @GetMapping("/professor")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Get professor dashboard statistics",
            description = "Returns statistics for the logged-in professor's dashboard")
    public ResponseEntity<ApiResponse<ProfessorStatsDTO>> getProfessorStats() {
        User user = getCurrentUser();
        Professor professor = professorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Professor profile not found"));
        ProfessorStatsDTO stats = statsService.getProfessorStats(professor.getId());
        return ResponseEntity.ok(ApiResponse.success("Professor stats retrieved", stats));
    }

    // ==========================================
    // Student Stats
    // ==========================================

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student dashboard statistics",
            description = "Returns statistics for the logged-in student's dashboard")
    public ResponseEntity<ApiResponse<StudentStatsDTO>> getStudentStats() {
        User user = getCurrentUser();
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        StudentStatsDTO stats = statsService.getStudentStats(student.getId());
        return ResponseEntity.ok(ApiResponse.success("Student stats retrieved", stats));
    }

    // ==========================================
    // Utility
    // ==========================================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString();
        }

        // Students authenticate with uniqueCode, not email
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Student student = studentRepository.findByUniqueCode(username)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            return student.getUser();
        }

        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
