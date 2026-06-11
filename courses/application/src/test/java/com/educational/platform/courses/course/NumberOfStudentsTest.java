package com.educational.platform.courses.course;

import com.educational.platform.common.domain.ValueObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberOfStudentsTest {

    @Test
    void constructor_positiveNumber_storedCorrectly() {
        // when
        final NumberOfStudents sut = new NumberOfStudents(10);

        // then
        assertThat(sut.number()).isEqualTo(10);
    }

    @Test
    void constructor_zero_storedCorrectly() {
        // when
        final NumberOfStudents sut = new NumberOfStudents(0);

        // then
        assertThat(sut.number()).isEqualTo(0);
    }

    @Test
    void constructor_negativeNumber_storedCorrectly() {
        // when
        final NumberOfStudents sut = new NumberOfStudents(-1);

        // then
        assertThat(sut.number()).isEqualTo(-1);
    }

    @Test
    void constructor_maxInt_storedCorrectly() {
        // when
        final NumberOfStudents sut = new NumberOfStudents(Integer.MAX_VALUE);

        // then
        assertThat(sut.number()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void constructor_minInt_storedCorrectly() {
        // when
        final NumberOfStudents sut = new NumberOfStudents(Integer.MIN_VALUE);

        // then
        assertThat(sut.number()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void implementsValueObject() {
        // then
        assertThat(ValueObject.class).isAssignableFrom(NumberOfStudents.class);
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final NumberOfStudents a = new NumberOfStudents(5);
        final NumberOfStudents b = new NumberOfStudents(5);

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentInstances_areNotEqual() {
        // given
        final NumberOfStudents a = new NumberOfStudents(5);
        final NumberOfStudents b = new NumberOfStudents(10);

        // then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_containsNumber() {
        // given
        final NumberOfStudents sut = new NumberOfStudents(42);

        // then
        assertThat(sut.toString()).contains("42");
    }
}
