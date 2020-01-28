package com.github.egnaf.auth.configs.security;

import com.github.egnaf.auth.exceptions.AuthenticationException;
import com.github.egnaf.auth.models.RoleModel;
import com.github.egnaf.auth.utils.helpers.RandomHelper;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TokenProvider {

    @Value("${security.token.secret:secret}")
    private String secretKey;

    @Value("${security.token.expire:3600000}")
    private long expire = 3600000;

    @Value("${security.token.type:bearer}")
    private String tokenType = "bearer";

    private final UserDetailsService userDetailsService;

    @Autowired
    public TokenProvider(@Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createAccessToken(String username, Set<RoleModel> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("auth", roles.stream()
                .map(s -> new SimpleGrantedAuthority(s.getAuthority()))
                .collect(Collectors.toList()));
        Date currentDate = new Date();
        Date expiresInDate = new Date(currentDate.getTime() + expire);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(currentDate)
                .setExpiration(expiresInDate)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String createRefreshToken() {
        return RandomHelper.generate();
    }

    public long getExpire() {
        return new Timestamp(System.currentTimeMillis()).getTime() + expire;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Authentication authenticateToken(String token) {
        UserDetails userDetails;
        String username = getUsernameByToken(token);
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
            return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        } catch (UsernameNotFoundException e) {
            throw new AuthenticationException("The username " + username + " not found", HttpStatus.UNAUTHORIZED);
        }
    }

    public String getUsernameByToken(String token) {
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Expired token", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            throw new AuthenticationException("Invalid token", HttpStatus.BAD_REQUEST);
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("Wrong token", HttpStatus.BAD_REQUEST);
        } catch (SignatureException e) {
            throw new AuthenticationException("Unverified token", HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Illegal argument", HttpStatus.BAD_REQUEST);
        }
    }

    public String extractToken(HttpServletRequest req) {
        String token = req.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Expired token", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            throw new AuthenticationException("Invalid token", HttpStatus.BAD_REQUEST);
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("Wrong token", HttpStatus.BAD_REQUEST);
        } catch (SignatureException e) {
            throw new AuthenticationException("Unverified token", HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Illegal argument", HttpStatus.BAD_REQUEST);
        }
    }
}
