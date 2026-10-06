package com.educational.platform.course.enrollments;

/**
 * Represents the completion status of the enrollment.
 */
public enum CompletionStatus {

    IN_PROGRESS,
    COMPLETED;

    public CompletionStatusDTO toDTO() {
        switch (this) {
            case IN_PROGRESS:
                return CompletionStatusDTO.IN_PROGRESS;
            case COMPLETED:
                return CompletionStatusDTO.COMPLETED;
        }

        return null;
    }
}
