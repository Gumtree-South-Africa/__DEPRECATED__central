package com.ecg.replyts.core.runtime.csv;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;

public class CsvUtilsTest {

    @Test
    public void toCsv_whenIterableNull_shouldReturnEmptyString() {
        String actual = CsvUtils.toCsv(null);
        Assertions.assertThat(actual).isEmpty();
    }

    @Test
    public void toCsv_whenIterableEmpty_shouldReturnEmptyString() {
        String actual = CsvUtils.toCsv(Collections.emptyList());
        Assertions.assertThat(actual).isEmpty();
    }

    @Test
    public void toCsv_whenElementsNull_shouldReturnEmptyString() {
        String actual = CsvUtils.toCsv(Arrays.asList(null, null));
        Assertions.assertThat(actual).isEmpty();
    }

    @Test
    public void toCsv_whenResultNotEmpty_shouldNotHaveTrailingNewLine() {
        String actual = CsvUtils.toCsv(Arrays.asList(configureMock("0,1"), configureMock("1,0")));
        Assertions.assertThat(actual).isEqualTo("0,1\n1,0");
    }

    @Test
    public void toCsv_whenContainsNullElements_shouldSkipThose() {
        String actual = CsvUtils.toCsv(Arrays.asList(configureMock("1,0,null"), null, null, configureMock("null,1,something")));
        Assertions.assertThat(actual).isEqualTo("1,0,null\nnull,1,something");
    }

    private CsvSerializable configureMock(String toBeReturned) {
        CsvSerializable csvSerializableMock = Mockito.mock(CsvSerializable.class);
        when(csvSerializableMock.toCsvLine()).thenReturn(toBeReturned);
        return csvSerializableMock;
    }
}
