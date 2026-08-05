package com.educational.platform.courses.course.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.educational.platform.courses.course.CourseDTO;
import com.educational.platform.courses.course.CourseRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseByUUIDQueryHandlerTest {

	@Mock
	private CourseRepository repository;

	@InjectMocks
	private CourseByUUIDQueryHandler sut;

	@Test
	void handle_existingUuid_correspondingDtoReturned() {
		// given
		var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		var dto = new CourseDTO(uuid, "name", "description", 5, List.of());
		when(repository.findDTOByUuid(uuid)).thenReturn(Optional.of(dto));

		// when
		var result = sut.handle(new CourseByUUIDQuery(uuid));

		// then
		assertThat(result).contains(dto);
	}

	@Test
	void handle_missingUuid_emptyOptional() {
		// given
		var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440999");
		when(repository.findDTOByUuid(uuid)).thenReturn(Optional.empty());

		// when
		var result = sut.handle(new CourseByUUIDQuery(uuid));

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void handle_method_annotatedWithReadOnlyTransaction() throws NoSuchMethodException {
		// when
		var transactional = CourseByUUIDQueryHandler.class.getMethod("handle", CourseByUUIDQuery.class).getAnnotation(Transactional.class);

		// then
		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isTrue();
	}
}
