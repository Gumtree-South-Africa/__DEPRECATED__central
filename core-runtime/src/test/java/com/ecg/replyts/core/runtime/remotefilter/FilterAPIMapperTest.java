package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.comaas.filterapi.dto.FilterFeedback;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

public class FilterAPIMapperTest {
    private Set<FilterFeedback.ResultStateEnum> apiValues = new HashSet<>(Arrays.asList(FilterFeedback.ResultStateEnum.values()));

    @Test
    public void allApiValuesAreMapped() {
        apiValues.forEach(this::assertHasModel);
    }

    private void assertHasModel(FilterFeedback.ResultStateEnum apiValue) {
        FilterResultState modelValue = FilterAPIMapper.FromAPI.toModelResultState(apiValue);

        assertNotNull("API value must be mapped to model", modelValue);
    }

}