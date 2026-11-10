package com.nguyen.motoparts.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.AccountLockRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Đọc "Authorization: Bearer ...", kiểm tra token rồi nạp tài khoản từ CSDL ở MỖI request.
 * Nhờ vậy tài khoản bị khóa (có dòng trong AccountLock) hoặc bị đổi quyền có hiệu lực ngay,
 * dù token vẫn còn hạn.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AccountLockRepository accountLockRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   AccountLockRepository accountLockRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.accountLockRepository = accountLockRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        Optional<String> accountId = jwtService.parseAccountId(header.substring(7).trim());
        Optional<User> user = accountId.flatMap(userRepository::findById);
        if (user.isEmpty()) {
            // Token sai, hết hạn hoặc tài khoản không còn: bỏ qua, đường cần đăng nhập sẽ trả 401.
            chain.doFilter(request, response);
            return;
        }

        User u = user.get();
        if (accountLockRepository.existsById(u.getAccountId())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of("message", "Tài khoản đã bị khóa."));
            return;
        }

        AuthUser principal = new AuthUser(u.getAccountId(), u.getEmail(), u.getUserRole());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getUserRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
