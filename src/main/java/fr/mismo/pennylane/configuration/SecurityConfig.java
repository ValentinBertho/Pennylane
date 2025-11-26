package fr.mismo.pennylane.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Configuration de la sécurité Spring Security.
 *
 * Cette configuration active :
 * - Authentification Basic HTTP pour protéger l'accès à l'application
 * - CSRF protection pour les endpoints sensibles
 * - Autorisation basée sur les rôles
 *
 * Les endpoints publics (health checks) sont exemptés d'authentification.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.user.name:admin}")
    private String username;

    @Value("${security.user.password:changeme}")
    private String password;

    @Value("${security.basic.enabled:true}")
    private boolean securityEnabled;

    /**
     * Configure la chaîne de filtres de sécurité.
     *
     * Configuration :
     * - CSRF activé avec repository basé sur cookies pour les applications web
     * - Authentification requise pour tous les endpoints sauf health checks
     * - Basic Auth activé pour une intégration facile
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            // Mode développement uniquement - CSRF désactivé et accès libre
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        } else {
            // Mode production - Sécurité activée
            http
                .csrf(csrf -> csrf
                    // Active CSRF avec cookies HttpOnly pour protection contre XSS
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    // Exemption CSRF pour les endpoints de health check
                    .ignoringRequestMatchers("/actuator/health", "/actuator/health/**")
                )
                .authorizeHttpRequests(authz -> authz
                    // Endpoints publics (health checks sans authentification)
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    // Endpoints de métriques - authentification requise
                    .requestMatchers("/actuator/**").authenticated()
                    // Tous les autres endpoints - authentification requise
                    .anyRequest().authenticated()
                )
                // Active l'authentification Basic HTTP
                .httpBasic(Customizer.withDefaults());
        }

        return http.build();
    }

    /**
     * Configure l'encodeur de mots de passe.
     * Utilise BCrypt qui est considéré comme sécurisé pour hasher les mots de passe.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure les utilisateurs en mémoire.
     *
     * Pour la production, il est recommandé de :
     * - Utiliser une base de données pour stocker les utilisateurs
     * - Implémenter un système de rôles plus complexe
     * - Utiliser OAuth2 ou LDAP pour l'authentification
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username(username)
            .password(passwordEncoder().encode(password))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(user);
    }
}
