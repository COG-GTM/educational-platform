package com.educational.platform.administration.course.query;

import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.inject.Named;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalRepository;

/**
 * Query handler for getting the course proposals by uuid.
 */
@Named
public class ListCourseProposalsQueryHandler {

	private final CourseProposalRepository repository;

    public ListCourseProposalsQueryHandler(CourseProposalRepository repository) {
        this.repository = repository;
    }

    /**
	 * Retrieves a page of course proposals.
	 *
	 * @param query query.
	 * @return page of course proposals.
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@Nonnull
	public Page<CourseProposalDTO> handle(ListCourseProposalsQuery query) {
		return repository.listCourseProposals(PageRequest.of(query.page(), query.size()));
	}

}
