package com.taskapi.security.auth;

import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import com.taskapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService service;

    private User makeUser(Long id, String name, String email, Role role) {
        try {
            var user = new User(name, email, "hashed-password");
            user.setRole(role);
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── loadUserByUsername ──────────────────────────────────────────────────

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadByUsername {

        @Test
        @DisplayName("retorna UserDetails quando email existe")
        void shouldReturnUserDetailsWhenFound() {
            var user = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));

            var details = service.loadUserByUsername("ana@email.com");

            assertThat(details.getUsername()).isEqualTo("ana@email.com");
            assertThat(details.getPassword()).isEqualTo("hashed-password");
            assertThat(details.getAuthorities()).hasSize(1);
            assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("lanca UsernameNotFoundException quando email nao existe")
        void shouldThrowWhenNotFound() {
            when(userRepository.findByEmail("nope@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("nope@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nope@email.com");
        }
    }

    // ─── loadUserById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("loadUserById()")
    class LoadById {

        @Test
        @DisplayName("retorna CustomUserDetails quando id existe")
        void shouldReturnWhenFound() {
            var user = makeUser(1L, "Ana", "ana@email.com", Role.ADMIN);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            var details = service.loadUserById(1L);

            assertThat(details.getId()).isEqualTo(1L);
            assertThat(details.getUser().getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("lanca UsernameNotFoundException quando id nao existe")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserById(99L))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("99");
        }
    }
}
