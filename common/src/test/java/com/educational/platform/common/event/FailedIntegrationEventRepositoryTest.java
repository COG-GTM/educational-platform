package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FailedIntegrationEventRepositoryTest {

    @Test
    void repository_isInterface() {
        assertThat(FailedIntegrationEventRepository.class.isInterface()).isTrue();
    }

    @Test
    void repository_isPublic() {
        assertThat(Modifier.isPublic(FailedIntegrationEventRepository.class.getModifiers())).isTrue();
    }

    @Test
    void repository_extendsJpaRepository() {
        assertThat(JpaRepository.class).isAssignableFrom(FailedIntegrationEventRepository.class);
    }

    @Test
    void repository_genericEntityTypeIsFailedIntegrationEventRecord() {
        Type[] genericInterfaces = FailedIntegrationEventRepository.class.getGenericInterfaces();
        ParameterizedType jpaRepoType = Arrays.stream(genericInterfaces)
                .filter(t -> t instanceof ParameterizedType)
                .map(t -> (ParameterizedType) t)
                .filter(t -> t.getRawType() == JpaRepository.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Should extend JpaRepository"));

        assertThat(jpaRepoType.getActualTypeArguments()[0])
                .as("Entity type argument should be FailedIntegrationEventRecord")
                .isEqualTo(FailedIntegrationEventRecord.class);
    }

    @Test
    void repository_genericIdTypeIsLong() {
        Type[] genericInterfaces = FailedIntegrationEventRepository.class.getGenericInterfaces();
        ParameterizedType jpaRepoType = Arrays.stream(genericInterfaces)
                .filter(t -> t instanceof ParameterizedType)
                .map(t -> (ParameterizedType) t)
                .filter(t -> t.getRawType() == JpaRepository.class)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Should extend JpaRepository"));

        assertThat(jpaRepoType.getActualTypeArguments()[1])
                .as("ID type argument should be Long")
                .isEqualTo(Long.class);
    }

    @Test
    void repository_hasNoDeclaredMethods() {
        assertThat(FailedIntegrationEventRepository.class.getDeclaredMethods())
                .as("Repository should inherit all methods from JpaRepository without custom additions")
                .isEmpty();
    }

    @Test
    void repository_doesNotHaveNoRepositoryBeanAnnotation() {
        assertThat(FailedIntegrationEventRepository.class.getAnnotation(NoRepositoryBean.class)).isNull();
    }

    @Test
    void repository_hasNoDeclaredFields() {
        assertThat(FailedIntegrationEventRepository.class.getDeclaredFields())
                .as("Repository interface should have no fields")
                .isEmpty();
    }
}
