package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.response.UserSummaryResponse;
import com.tlc.surveyexchange_backend.exception.ResourceNotFoundException;
import com.tlc.surveyexchange_backend.model.User;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .name(user.getName())
                .email(user.getEmail())
                .creditBalance(user.getCreditBalance())
                .build();
    }
}
