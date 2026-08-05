package com.educational.platform.administration.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents course proposal repository.
 */
public interface CourseProposalRepository extends JpaRepository<CourseProposal, Integer> {

    /**
     * Retrieves a course proposal by its uuid.
     *
     * @param uuid must not be {@literal null}.
     * @return the course proposal with the given uuid or {@literal Optional#empty()} if none found.
     * @throws IllegalArgumentException if {@literal uuid} is {@literal null}.
     */
    Optional<CourseProposal> findByUuid(UUID uuid);

    /**
     * Retrieves a page of course proposals.
     *
     * @param pageable pagination information, must not be {@literal null}.
     * @return page of course proposals.
     */
    @Query("select new com.educational.platform.administration.course.CourseProposalDTO(cp.uuid, cp.status) from CourseProposal cp order by cp.id")
    Page<CourseProposalDTO> listCourseProposals(Pageable pageable);

}
