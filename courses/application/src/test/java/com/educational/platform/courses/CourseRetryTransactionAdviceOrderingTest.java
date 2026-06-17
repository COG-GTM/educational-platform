package com.educational.platform.courses;

import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the advice ordering the retry feature depends on: the {@code @Retryable} advice must wrap
 * <em>outside</em> the {@code @Transactional} advice on the course command handlers, so every retry
 * attempt runs in its own fresh transaction (load -&gt; mutate -&gt; save). If the order were
 * reversed, all attempts would share a single transaction/persistence context, the retried read
 * would never observe the committed conflicting version, and the optimistic-locking recovery would
 * silently never succeed.
 *
 * <p>The existing retry tests prove the {@code @Retryable} mechanics with a mocked repository, but
 * none assert that retry is the outermost advice; this test wires the production
 * {@link CourseRetryConfiguration} ({@code @EnableRetry}) together with transaction management so both
 * the retry and transaction interceptors are present on each handler proxy, then inspects the advisor
 * chain directly.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetryTransactionAdviceOrderingTest.AdviceConfig.class})
class CourseRetryTransactionAdviceOrderingTest {

    @Configuration
    @EnableTransactionManagement
    static class AdviceConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        CourseRepository courseRepository() {
            return mock(CourseRepository.class);
        }

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler(CourseRepository repository) {
            return new IncreaseNumberOfStudentsCommandHandler(repository);
        }

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler(CourseRepository repository) {
            return new UpdateCourseRatingCommandHandler(repository);
        }
    }

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Test
    void increaseNumberOfStudentsHandler_retryAdviceWrapsOutsideTransactionAdvice() {
        assertRetryWrapsOutsideTransaction(increaseNumberOfStudentsCommandHandler);
    }

    @Test
    void updateCourseRatingHandler_retryAdviceWrapsOutsideTransactionAdvice() {
        assertRetryWrapsOutsideTransaction(updateCourseRatingCommandHandler);
    }

    private static void assertRetryWrapsOutsideTransaction(Object handler) {
        assertThat(AopUtils.isAopProxy(handler))
                .as("handler must be an AOP proxy carrying the retry and transaction advice")
                .isTrue();

        final Advisor[] advisors = ((Advised) handler).getAdvisors();

        Integer retryIndex = null;
        Integer transactionIndex = null;
        for (int i = 0; i < advisors.length; i++) {
            final Advice advice = advisors[i].getAdvice();
            if (advice instanceof TransactionInterceptor) {
                transactionIndex = i;
            } else if (advice.getClass().getName().contains("Retry")) {
                retryIndex = i;
            }
        }

        assertThat(retryIndex).as("@Retryable advice must be applied to the handler").isNotNull();
        assertThat(transactionIndex).as("@Transactional advice must remain applied to the handler").isNotNull();
        // advisors are ordered outermost-first, so the outer advice has the lower index
        assertThat(retryIndex)
                .as("@Retryable must wrap outside @Transactional so each retry attempt is a fresh transaction")
                .isLessThan(transactionIndex);
    }
}
