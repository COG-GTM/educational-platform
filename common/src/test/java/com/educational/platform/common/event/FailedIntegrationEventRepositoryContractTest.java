package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the contract of {@link FailedIntegrationEventRepository}.
 * <p>
 * The repository must be a plain Spring Data JPA repository with no custom query methods.
 * Custom methods would require additional test coverage and could introduce subtle bugs
 * (e.g., N+1 queries, missing indexes). If the team needs custom queries in the future,
 * this test will flag the addition and prompt test coverage for the new methods.
 */
class FailedIntegrationEventRepositoryContractTest {

    @Test
    void repository_extendsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(FailedIntegrationEventRepository.class);
    }

    @Test
    void repository_isAnInterface() {
        assertThat(FailedIntegrationEventRepository.class.isInterface()).isTrue();
    }

    @Test
    void repository_entityTypeIsFailedIntegrationEventRecord() {
        Type[] genericInterfaces = FailedIntegrationEventRepository.class.getGenericInterfaces();
        assertThat(genericInterfaces).hasSize(1);

        ParameterizedType jpaRepoType = (ParameterizedType) genericInterfaces[0];
        Type entityType = jpaRepoType.getActualTypeArguments()[0];
        assertThat(entityType).isEqualTo(FailedIntegrationEventRecord.class);
    }

    @Test
    void repository_idTypeIsLong() {
        Type[] genericInterfaces = FailedIntegrationEventRepository.class.getGenericInterfaces();
        ParameterizedType jpaRepoType = (ParameterizedType) genericInterfaces[0];
        Type idType = jpaRepoType.getActualTypeArguments()[1];
        assertThat(idType).isEqualTo(Long.class);
    }

    @Test
    void repository_hasNoCustomDeclaredMethods() {
        Method[] declaredMethods = FailedIntegrationEventRepository.class.getDeclaredMethods();
        assertThat(declaredMethods)
                .as("Repository should have no custom query methods — uses only inherited JpaRepository methods")
                .isEmpty();
    }

    @Test
    void repository_doesNotHaveNoRepositoryBeanAnnotation() {
        assertThat(FailedIntegrationEventRepository.class.getAnnotation(NoRepositoryBean.class))
                .as("Repository must NOT be annotated with @NoRepositoryBean — it must be instantiated by Spring Data")
                .isNull();
    }

    @Test
    void repository_isPublic() {
        assertThat(java.lang.reflect.Modifier.isPublic(FailedIntegrationEventRepository.class.getModifiers()))
                .as("Repository must be public for Spring Data to create the proxy")
                .isTrue();
    }

    @Test
    void repository_directlyExtendsJpaRepository() {
        Class<?>[] interfaces = FailedIntegrationEventRepository.class.getInterfaces();
        assertThat(interfaces)
                .as("Repository should directly extend JpaRepository without intermediary interfaces")
                .hasSize(1)
                .containsExactly(JpaRepository.class);
    }

    @Test
    void repository_packageIsCommonEvent() {
        assertThat(FailedIntegrationEventRepository.class.getPackageName())
                .isEqualTo("com.educational.platform.common.event");
    }

    @Test
    void repository_hasNoDeclaredFields() {
        assertThat(FailedIntegrationEventRepository.class.getDeclaredFields())
                .as("Interface should have no fields")
                .isEmpty();
    }

    @Test
    void repository_hasNoDefaultMethods() {
        long defaultMethodCount = Arrays.stream(FailedIntegrationEventRepository.class.getDeclaredMethods())
                .filter(Method::isDefault)
                .count();
        assertThat(defaultMethodCount)
                .as("Repository should not define default methods — keep query logic in Spring Data")
                .isZero();
    }
}
