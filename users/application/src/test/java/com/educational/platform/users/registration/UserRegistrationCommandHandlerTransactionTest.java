package com.educational.platform.users.registration;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(UserRegistrationCommandHandlerTransactionTest.Config.class)
class UserRegistrationCommandHandlerTransactionTest {

    @Autowired
    private UserRegistrationCommandHandler sut;

    @Autowired
    private UserRepository repository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PublishProbe publishProbe;

    @Test
    void handle_publishesEventInsideActiveTransaction() {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken("username", Collections.singletonList(Role.ROLE_STUDENT))).thenReturn("token");

        final String token = sut.handle(command);

        assertThat(token).isEqualTo("token");
        assertThat(publishProbe.transactionActiveDuringPublish.get()).isTrue();
        assertThat(publishProbe.event.get()).isEqualTo(new UserCreatedIntegrationEvent("username", "email@gmail.com"));
        verify(repository).existsByUsername("username");
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        UserRepository repository() {
            return mock(UserRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        PublishProbe publishProbe() {
            return new PublishProbe();
        }

        @Bean
        UserRegistrationCommandHandler userRegistrationCommandHandler(
                PlatformTransactionManager transactionManager,
                PasswordEncoder passwordEncoder,
                JwtTokenProvider jwtTokenProvider,
                UserRepository repository,
                ApplicationEventPublisher eventPublisher,
                Validator validator) {
            return new UserRegistrationCommandHandler(new TransactionTemplate(transactionManager), passwordEncoder, jwtTokenProvider, repository, eventPublisher, validator);
        }
    }

    static class PublishProbe {

        private final AtomicBoolean transactionActiveDuringPublish = new AtomicBoolean();
        private final AtomicReference<UserCreatedIntegrationEvent> event = new AtomicReference<>();

        @EventListener
        void handle(UserCreatedIntegrationEvent event) {
            transactionActiveDuringPublish.set(TransactionSynchronizationManager.isActualTransactionActive());
            this.event.set(event);
        }
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
