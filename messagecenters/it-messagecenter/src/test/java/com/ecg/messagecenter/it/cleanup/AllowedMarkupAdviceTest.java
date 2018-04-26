package com.ecg.messagecenter.it.cleanup;

import com.ecg.messagecenter.it.cleanup.AllowedMarkupAdvice;
import com.ecg.messagecenter.it.cleanup.Text;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(Parameterized.class) public class AllowedMarkupAdviceTest {

    private String message;
    private String expected;

    public AllowedMarkupAdviceTest(String message, String expected) {
        this.message = message;
        this.expected = expected;
    }

    @Parameterized.Parameters public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{"Mi illumino\ndi immenso!\n- Ungaretti",
                        "Mi illumino\ndi immenso!\n- Ungaretti"},
                        {"Mi illumino\n\ndi immenso!\n\n\n\n\n- Ungaretti",
                                        "Mi illumino\ndi immenso!\n- Ungaretti"},
                        {"Mi illumino<br/>di immenso<br />e tante care cose.<BR>- Ungarotty",
                                        "Mi illumino\ndi immenso\ne tante care cose.\n- Ungarotty"},
                        {"Mi illumino&lt;br/&gt;di immenso&lt;br /&gt;e ciao.&lt;BR&gt;- Ungarettow",
                                        "Mi illumino\ndi immenso\ne ciao.\n- Ungarettow"}});
    }

    @Test public void itShouldProcessMessageInTheRightWay() {
        Text text = fromString(this.message);

        AllowedMarkupAdvice advice = new AllowedMarkupAdvice(text);
        advice.processAdvice();

        assertThat(text.getAsString(), is(this.expected));
    }

    private Text fromString(String message) {
        List<Text.Line> list = Arrays.asList(new Text.Line(message, 0));

        return new Text(1, list);
    }
}
