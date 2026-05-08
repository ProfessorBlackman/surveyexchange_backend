package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.response.UserSummaryResponse;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.User;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSummaryResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .creditBalance(user.getCreditBalance())
                .institution(user.getInstitution())
                .fieldOfStudy(user.getFieldOfStudy())
                .academicLevel(user.getAcademicLevel())
                .yearOfStudy(user.getYearOfStudy())
                .gender(user.getGender())
                .ageRange(user.getAgeRange())
                .build();
    }

    @Async
    @Transactional
    public void updateLastActiveAsync(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            if (user.getLastActiveAt() == null || user.getLastActiveAt().isBefore(fiveMinutesAgo)) {
                user.setLastActiveAt(Instant.now());
                userRepository.save(user);
            }
        });
    }
}
