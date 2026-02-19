package com.free.easyLearn.specification;

import com.free.easyLearn.entity.Quiz;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizSpecification {

    public static Specification<Quiz> withFilters(UUID sessionId, String language, Boolean isPublished, UUID createdBy, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sessionId != null) {
                predicates.add(cb.equal(root.get("session").get("id"), sessionId));
            }

            if (language != null) {
                predicates.add(cb.equal(root.get("language"), language));
            }

            if (isPublished != null) {
                predicates.add(cb.equal(root.get("isPublished"), isPublished));
            }

            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), createdBy));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
