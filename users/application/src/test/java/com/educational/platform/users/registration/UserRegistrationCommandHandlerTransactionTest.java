package com.educational.platform.users.registration;

import com.educational.platform.common.outbox.IntegrationEventOutbox;
import com.educational.platform.common.outbox.IntegrationEventOutboxEntry;
import com.educational.platform.common.outbox.IntegrationEventOutboxRepository;
import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import com.educational.platform.users.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.ArgumentMatchers.any;
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
    private OutboxSaveProbe outboxSaveProbe;

    @Test
    void handle_storesEventInOutboxInsideActiveTransaction() throws Exception {
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
        assertThat(outboxSaveProbe.transactionActiveDuringSave.get()).isTrue();
        final IntegrationEventOutboxEntry entry = outboxSaveProbe.entry.get();
        assertThat(entry.getEventType()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
        assertThat(new ObjectMapper().readValue(entry.getPayload(), UserCreatedIntegrationEvent.class))
                .isEqualTo(new UserCreatedIntegrationEvent("username", "email@gmail.com"));
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
        OutboxSaveProbe outboxSaveProbe() {
            return new OutboxSaveProbe();
        }

        @Bean
        IntegrationEventOutboxRepository outboxRepository(OutboxSaveProbe outboxSaveProbe) {
            final IntegrationEventOutboxRepository outboxRepository = mock(IntegrationEventOutboxRepository.class);
            when(outboxRepository.save(any(IntegrationEventOutboxEntry.class))).thenAnswer(invocation -> {
                outboxSaveProbe.transactionActiveDuringSave.set(TransactionSynchronizationManager.isActualTransactionActive());
                outboxSaveProbe.entry.set(invocation.getArgument(0));
                return invocation.getArgument(0);
            });
            return outboxRepository;
        }

        @Bean
        IntegrationEventOutbox integrationEventOutbox(IntegrationEventOutboxRepository outboxRepository) {
            return new IntegrationEventOutbox(outboxRepository);
        }

        @Bean
        UserRegistrationCommandHandler userRegistrationCommandHandler(
                PlatformTransactionManager transactionManager,
                PasswordEncoder passwordEncoder,
                JwtTokenProvider jwtTokenProvider,
                UserRepository repository,
                IntegrationEventOutbox integrationEventOutbox,
                Validator validator) {
            return new UserRegistrationCommandHandler(new TransactionTemplate(transactionManager), passwordEncoder, jwtTokenProvider, repository, integrationEventOutbox, validator);
        }
    }

    static class OutboxSaveProbe {

        private final AtomicBoolean transactionActiveDuringSave = new AtomicBoolean();
        private final AtomicReference<IntegrationEventOutboxEntry> entry = new AtomicReference<>();
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return TransactionSynchronizationManager.isActualTransactionActive();
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
