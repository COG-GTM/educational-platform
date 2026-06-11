package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnumsTest {

    @Test
    void approvalStatus_containsExpectedValues() {
        assertThat(ApprovalStatus.values())
                .containsExactly(
                        ApprovalStatus.NOT_SENT_FOR_APPROVAL,
                        ApprovalStatus.WAITING_FOR_APPROVAL,
                        ApprovalStatus.DECLINED,
                        ApprovalStatus.APPROVED);
        assertThat(ApprovalStatus.valueOf("APPROVED")).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void publishStatus_containsExpectedValues() {
        assertThat(PublishStatus.values())
                .containsExactly(PublishStatus.DRAFT, PublishStatus.PUBLISHED, PublishStatus.ARCHIVED);
        assertThat(PublishStatus.valueOf("DRAFT")).isEqualTo(PublishStatus.DRAFT);
    }

    @Test
    void lectureType_containsExpectedValues() {
        assertThat(LectureType.values()).containsExactly(LectureType.TEXT);
        assertThat(LectureType.valueOf("TEXT")).isEqualTo(LectureType.TEXT);
    }
}
