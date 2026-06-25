package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

class FailedIntegrationEventRepositoryTest {

    @Test
    void interface_extendsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(FailedIntegrationEventRepository.class);
    }

    @Test
    void interface_isAnInterface() {
        assertThat(FailedIntegrationEventRepository.class.isInterface()).isTrue();
    }

    @Test
    void interface_hasCorrectEntityType() {
        // when
        Type[] genericInterfaces = FailedIntegrationEventRepository.class.getGenericInterfaces();

        // then
        assertThat(genericInterfaces).hasSize(1);
        ParameterizedType jpaRepoType = (ParameterizedType) genericInterfaces[0];
        Type[] typeArguments = jpaRepoType.getActualTypeArguments();
        assertThat(typeArguments[0]).isEqualTo(FailedIntegrationEventRecord.class);
    }

    @Test
    void interface_hasCorrectIdType() {
        // when
        Type[] genericInterfaces = FailedIntegrationEventRepository.class.getGenericInterfaces();

        // then
        ParameterizedType jpaRepoType = (ParameterizedType) genericInterfaces[0];
        Type[] typeArguments = jpaRepoType.getActualTypeArguments();
        assertThat(typeArguments[1]).isEqualTo(Long.class);
    }

    @Test
    void interface_inheritsStandardCrudMethods() throws NoSuchMethodException {
        // JpaRepository provides save, findById, findAll, delete, etc.
        assertThat(FailedIntegrationEventRepository.class.getMethod("save", Object.class)).isNotNull();
        assertThat(FailedIntegrationEventRepository.class.getMethod("findById", Object.class)).isNotNull();
        assertThat(FailedIntegrationEventRepository.class.getMethod("findAll")).isNotNull();
        assertThat(FailedIntegrationEventRepository.class.getMethod("deleteById", Object.class)).isNotNull();
    }

    @Test
    void interface_doesNotDeclareCustomMethods() {
        // FailedIntegrationEventRepository should only inherit from JpaRepository without custom queries
        assertThat(FailedIntegrationEventRepository.class.getDeclaredMethods()).isEmpty();
    }
}
