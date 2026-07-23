package com.educational.platform.messaging;

import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the messaging flow between the users and courses bounded contexts:
 * user registration publishes {@link UserCreatedIntegrationEvent} which is consumed
 * asynchronously by the courses context to replicate the teacher.
 */
@Sql(scripts = "classpath:messaging/user_created_flow.sql")
public class UserCreatedIntegrationEventFlowTest extends MessagingIntegrationTestSupport {

	@Autowired
	private UserRegistrationCommandHandler userRegistrationCommandHandler;

	@Autowired
	private TeacherRepository teacherRepository;

	@Test
	void registerTeacher_userCreatedEventPublished_teacherReplicatedInCoursesContext() {
		// given
		final UserRegistrationCommand command = UserRegistrationCommand.builder()
				.username("event-flow-teacher")
				.email("event-flow-teacher@gmail.com")
				.password("password123")
				.role(RoleDTO.ROLE_TEACHER)
				.build();

		// when
		userRegistrationCommandHandler.handle(command);

		// then
		assertThat(integrationEvents.eventsOfType(UserCreatedIntegrationEvent.class))
				.contains(new UserCreatedIntegrationEvent("event-flow-teacher", "event-flow-teacher@gmail.com"));

		awaitAsyncListeners(() ->
				assertThat(teacherRepository.findByUsername("event-flow-teacher")).isNotNull());
	}
}
