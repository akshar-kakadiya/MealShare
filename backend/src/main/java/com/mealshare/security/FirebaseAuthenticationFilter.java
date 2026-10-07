package com.mealshare.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseTokenValidator tokenValidator;
    private final UserRepository userRepository;

    public FirebaseAuthenticationFilter(FirebaseTokenValidator tokenValidator, UserRepository userRepository) {
        this.tokenValidator = tokenValidator;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(BEARER_PREFIX.length());

        try {
            FirebaseToken decodedToken = tokenValidator.verifyIdToken(idToken);

            FirebasePrincipal principal = new FirebasePrincipal(
                    decodedToken.getUid(),
                    decodedToken.getEmail(),
                    decodedToken.getName(),
                    decodedToken.getClaims() != null ? decodedToken.getClaims() : Map.of()
            );

            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional<User> userOpt = userRepository.findByFirebaseUid(principal.uid());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getStatus() == UserStatus.SUSPENDED) {
                    log.warn("User {} is suspended.", user.getId());
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUSPENDED"));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_UNREGISTERED"));
            }

            FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(
                    principal,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (FirebaseAuthException e) {
            log.warn("Firebase token verification failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Unexpected error during Firebase authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
