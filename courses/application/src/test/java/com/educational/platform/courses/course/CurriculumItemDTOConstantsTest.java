package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the string constants on {@link CurriculumItemDTO} used as alias keys
 * in the tuple-based result transformer.
 */
public class CurriculumItemDTOConstantsTest {

    @Test
    void typeConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.TYPE).isEqualTo("curriculumItems_type");
    }

    @Test
    void titleConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.TITLE).isEqualTo("curriculumItems_title");
    }

    @Test
    void descriptionConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.DESCRIPTION).isEqualTo("curriculumItems_description");
    }

    @Test
    void serialNumberConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.SERIAL_NUMBER).isEqualTo("curriculumItems_serialNumber");
    }
}
