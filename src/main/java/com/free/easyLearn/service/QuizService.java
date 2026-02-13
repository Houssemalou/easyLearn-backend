//package com.free.easyLearn.service;
//
//import com.lingua.hub.dto.quiz.*;
//import com.lingua.hub.entity.*;
//import com.lingua.hub.exception.BadRequestException;
//import com.lingua.hub.exception.ResourceNotFoundException;
//import com.lingua.hub.repository.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Service
//public class QuizService {
//
//    @Autowired
//    private QuizRepository quizRepository;
//
//    @Autowired
//    private QuizResultRepository quizResultRepository;
//
//    @Autowired
//    private RoomRepository roomRepository;
//
//    @Autowired
//    private StudentRepository studentRepository;
//
//    @Autowired
//    private ProfessorRepository professorRepository;
//
//    @Transactional
//    public QuizDTO createQuiz(CreateQuizRequest request, UUID professorId) {
//        Professor professor = professorRepository.findById(professorId)
//                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
//
//        Quiz quiz = Quiz.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .language(request.getLanguage())
//                .level(request.getLevel())
//                .timeLimit(request.getTimeLimit())
//                .passingScore(request.getPassingScore())
//                .isPublished(false)
//                .createdBy(professor)
//                .build();
//
//        if (request.getSessionId() != null) {
//            Room room = roomRepository.findById(request.getSessionId())
//                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
//            quiz.setSession(room);
//        }
//
//        // Add questions
//        List<QuizQuestion> questions = request.getQuestions().stream()
//                .map(q -> QuizQuestion.builder()
//                        .quiz(quiz)
//                        .question(q.getQuestion())
//                        .type(q.getType())
//                        .options(q.getOptions())
//                        .correctAnswer(q.getCorrectAnswer())
//                        .points(q.getPoints())
//                        .build())
//                .collect(Collectors.toList());
//        quiz.setQuestions(questions);
//
//        quiz = quizRepository.save(quiz);
//        return mapToDTO(quiz);
//    }
//
//    @Transactional
//    public QuizDTO publishQuiz(UUID quizId) {
//        Quiz quiz = quizRepository.findById(quizId)
//                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
//
//        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
//            throw new BadRequestException("Cannot publish quiz without questions");
//        }
//
//        quiz.setIsPublished(true);
//        quiz = quizRepository.save(quiz);
//        return mapToDTO(quiz);
//    }
//
//    @Transactional
//    public QuizResultDTO submitQuiz(UUID quizId, SubmitQuizRequest request, UUID studentId) {
//        Quiz quiz = quizRepository.findById(quizId)
//                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
//
//        Student student = studentRepository.findById(studentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
//
//        if (!quiz.getIsPublished()) {
//            throw new BadRequestException("Quiz is not published");
//        }
//
//        // Calculate score
//        int totalScore = 0;
//        int maxScore = quiz.getQuestions().stream()
//                .mapToInt(QuizQuestion::getPoints)
//                .sum();
//
//        List<QuizAnswer> answers = request.getAnswers().stream()
//                .map(a -> {
//                    QuizQuestion question = quiz.getQuestions().stream()
//                            .filter(q -> q.getId().equals(a.getQuestionId()))
//                            .findFirst()
//                            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
//
//                    boolean isCorrect = question.getCorrectAnswer().equals(a.getAnswer());
//
//                    return QuizAnswer.builder()
//                            .question(question)
//                            .answer(a.getAnswer())
//                            .isCorrect(isCorrect)
//                            .build();
//                })
//                .collect(Collectors.toList());
//
//        totalScore = answers.stream()
//                .filter(QuizAnswer::getIsCorrect)
//                .mapToInt(a -> a.getQuestion().getPoints())
//                .sum();
//
//        boolean passed = (totalScore * 100.0 / maxScore) >= quiz.getPassingScore();
//
//        QuizResult result = QuizResult.builder()
//                .quiz(quiz)
//                .student(student)
//                .score(totalScore)
//                .maxScore(maxScore)
//                .passed(passed)
//                .completedAt(LocalDateTime.now())
//                .timeSpent(request.getTimeSpent())
//                .build();
//
//        answers.forEach(a -> a.setResult(result));
//        result.setAnswers(answers);
//
//        result = quizResultRepository.save(result);
//        return mapResultToDTO(result);
//    }
//
//    public QuizDTO getQuizById(UUID quizId) {
//        Quiz quiz = quizRepository.findById(quizId)
//                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
//        return mapToDTO(quiz);
//    }
//
//    public Page<QuizDTO> getQuizzes(
//            UUID sessionId,
//            String language,
//            Student.LanguageLevel level,
//            Boolean isPublished,
//            UUID createdBy,
//            String search,
//            int page,
//            int size,
//            String sortBy,
//            String sortOrder
//    ) {
//        Sort sort = sortOrder.equalsIgnoreCase("desc")
//                ? Sort.by(sortBy).descending()
//                : Sort.by(sortBy).ascending();
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        Page<Quiz> quizzes = quizRepository.findByFilters(
//                sessionId, language, level, isPublished, createdBy, search, pageable
//        );
//
//        return quizzes.map(this::mapToDTO);
//    }
//
//    public List<QuizResultDTO> getQuizResults(UUID quizId) {
//        List<QuizResult> results = quizResultRepository.findByQuizId(quizId);
//        return results.stream()
//                .map(this::mapResultToDTO)
//                .collect(Collectors.toList());
//    }
//
//    public List<QuizResultDTO> getStudentQuizResults(UUID studentId) {
//        List<QuizResult> results = quizResultRepository.findByStudentId(studentId);
//        return results.stream()
//                .map(this::mapResultToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public void deleteQuiz(UUID quizId) {
//        Quiz quiz = quizRepository.findById(quizId)
//                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
//        quizRepository.delete(quiz);
//    }
//
//    private QuizDTO mapToDTO(Quiz quiz) {
//        List<QuizQuestionDTO> questions = quiz.getQuestions() != null
//                ? quiz.getQuestions().stream()
//                        .map(q -> QuizQuestionDTO.builder()
//                                .id(q.getId())
//                                .question(q.getQuestion())
//                                .type(q.getType())
//                                .options(q.getOptions())
//                                .correctAnswer(q.getCorrectAnswer())
//                                .points(q.getPoints())
//                                .build())
//                        .collect(Collectors.toList())
//                : List.of();
//
//        return QuizDTO.builder()
//                .id(quiz.getId())
//                .title(quiz.getTitle())
//                .description(quiz.getDescription())
//                .language(quiz.getLanguage())
//                .level(quiz.getLevel())
//                .timeLimit(quiz.getTimeLimit())
//                .passingScore(quiz.getPassingScore())
//                .isPublished(quiz.getIsPublished())
//                .sessionId(quiz.getSession() != null ? quiz.getSession().getId() : null)
//                .createdBy(quiz.getCreatedBy().getId())
//                .createdByName(quiz.getCreatedBy().getUser().getName())
//                .questions(questions)
//                .createdAt(quiz.getCreatedAt())
//                .build();
//    }
//
//    private QuizResultDTO mapResultToDTO(QuizResult result) {
//        List<QuizAnswerDTO> answers = result.getAnswers() != null
//                ? result.getAnswers().stream()
//                        .map(a -> QuizAnswerDTO.builder()
//                                .questionId(a.getQuestion().getId())
//                                .answer(a.getAnswer())
//                                .isCorrect(a.getIsCorrect())
//                                .build())
//                        .collect(Collectors.toList())
//                : List.of();
//
//        return QuizResultDTO.builder()
//                .id(result.getId())
//                .quizId(result.getQuiz().getId())
//                .quizTitle(result.getQuiz().getTitle())
//                .studentId(result.getStudent().getId())
//                .studentName(result.getStudent().getUser().getName())
//                .score(result.getScore())
//                .maxScore(result.getMaxScore())
//                .passed(result.getPassed())
//                .completedAt(result.getCompletedAt())
//                .timeSpent(result.getTimeSpent())
//                .answers(answers)
//                .build();
//    }
//}
