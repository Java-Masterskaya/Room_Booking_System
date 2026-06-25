package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.masterskaya.model.User;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {
    @Override
    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
            authentication instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("User is not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new InsufficientAuthenticationException("Invalid principal type");
        }

        return user;
    }
}
