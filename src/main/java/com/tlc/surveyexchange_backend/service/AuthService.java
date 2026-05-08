package com.tlc.surveyexchange_backend.service;

import com.tlc.surveyexchange_backend.dto.request.LoginRequest;
import com.tlc.surveyexchange_backend.dto.request.RegisterRequest;
import com.tlc.surveyexchange_backend.dto.response.AuthResponse;
import com.tlc.surveyexchange_backend.dto.response.UserSummaryResponse;
import com.tlc.surveyexchange_backend.exception.DuplicateResourceException;
import com.tlc.surveyexchange_backend.model.CreditTransaction;
import com.tlc.surveyexchange_backend.model.TransactionType;
import com.tlc.surveyexchange_backend.model.User;
import com.tlc.surveyexchange_backend.repository.CreditTransactionRepository;
import com.tlc.surveyexchange_backend.repository.UserRepository;
import com.tlc.surveyexchange_backend.security.JwtTokenProvider;
import com.tlc.surveyexchange_backend.security.UserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .institution(request.getInstitution())
                .fieldOfStudy(request.getFieldOfStudy())
                .academicLevel(request.getAcademicLevel())
                .yearOfStudy(request.getYearOfStudy())
                .gender(request.getGender())
                .ageRange(request.getAgeRange())
                .referralSource(request.getReferralSource())
                .creditBalance(10)
                .build();

        user = userRepository.save(user);

        CreditTransaction welcomeBonus = CreditTransaction.builder()
                .user(user)
                .amount(10)
                .type(TransactionType.SIGNUP_BONUS)
                .description("Welcome bonus on registration")
                .build();
        creditTransactionRepository.save(welcomeBonus);

        String token = jwtTokenProvider.generateToken(UserPrincipal.create(user));
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assert principal != null : new EntityNotFoundException("this user doesn't exist");
        User user = userRepository.findById(principal.getId())
                .orElseThrow();

        String token = jwtTokenProvider.generateToken(principal);
        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .user(UserSummaryResponse.builder()
                        .id(user.getId())
                        .name(user.getFullName())
                        .email(user.getEmail())
                        .creditBalance(user.getCreditBalance())
                        .build())
                .build();
    }
}
