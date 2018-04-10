package com.ecg.comaas.ebayk.filter.emailaddress;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    @Ignore
    public void currentlyNotSupported() {
        assertThat(normalizer.normalize("test._foo@bar.com")).contains("test.foo@bar.com");
        assertThat(normalizer.normalize("test (ät) testdotde")).contains("test@test.de");
        assertThat(normalizer.normalize("test@testpunktde")).contains("test@test.de");
    }

    @Test
    public void removeWhiteSpaces() {
        String normalized = normalizer.normalize("  test \t\n\r test ");
        assertThat(normalized).contains("testtest");
    }

    @Test
    public void exposeBug() {
        assertThat(normalizer.normalize("Marc.Schnabel@freenet.de")).contains("marc.schnabel@freenet.de");
    }

    @Test
    public void keepEmailAddress() {
        assertThat(normalizer.normalize("test@test.ebay.com")).contains("test@test.ebay.com");
    }

    @Test
    public void removeComments() {
        assertThat(normalizer.normalize("test(comment)@test.ebay.com")).contains("test@test.ebay.com");
    }

    @Test
    public void removeCommentsArountAt() {
        assertThat(normalizer.normalize("test(comment)(at)test.ebay.com")).contains("test@test.ebay.com");
    }

    @Test
    public void removeCommentsAroundDot() {
        assertThat(normalizer.normalize("test@test(comment)(dot)ebay.com")).contains("test@test.ebay.com");
    }


    @Test
    public void keepCommentsIncludingAt() {
        assertThat(normalizer.normalize("test(comment at test.de)@test.ebay.com")).contains("test@test.ebay.com");
    }

    @Test
    public void removeCommentsIncludingAt() {
        assertThat(normalizer.normalize("test(comment at test.de)@test.ebay.com")).contains("test@test.ebay.com");
    }

    @Test
    public void removeMultipleComments() {
        assertThat(normalizer.normalize("test(comment1)123(comment2)@(comment3)test.ebay.com")).contains("test123@test.ebay.com");
    }


    @Test
    public void keepEmailInBrackets() {
        assertThat(normalizer.normalize("(-- test@test.ebay.com --)")).isEqualTo("(--test@test.ebay.com--) ");
    }

    @Test
    public void keepMailToTag() {
        assertThat(normalizer.normalize("<a href=\"mailto:foo@bar.com\"")).contains("foo@bar.com");
    }

    @Test
    public void replaceAtPatterns() {
        assertThat(normalizer.normalize("test{at}test")).contains("test@test");
        assertThat(normalizer.normalize("test[at]test")).contains("test@test");
        assertThat(normalizer.normalize("test[et]test")).contains("test@test");
        assertThat(normalizer.normalize("test(at)test")).contains("test@test");
        assertThat(normalizer.normalize("test(et)test")).contains("test@test");
        assertThat(normalizer.normalize("test[ät]test")).contains("test@test");
        assertThat(normalizer.normalize("test(ät)test")).contains("test@test");
        assertThat(normalizer.normalize("test at test")).contains("test@test");
        assertThat(normalizer.normalize("test ät test")).contains("test@test");
        assertThat(normalizer.normalize("test_at_test")).contains("test@test");
        assertThat(normalizer.normalize("test#{at}#test")).contains("test@test");
        assertThat(normalizer.normalize("test©test")).contains("test@test");
        assertThat(normalizer.normalize("test®test")).contains("test@test");
        assertThat(normalizer.normalize("test(a)test.de")).contains("test@test.de");
        assertThat(normalizer.normalize("test()test.de")).contains("test@test.de");
        assertThat(normalizer.normalize("test ___ @ ___ test dot de")).contains("test@test.de");
    }

    @Test
    public void name() {
        assertThat(normalizer.normalize("text punkttest@test.de text")).contains("punkttest@test.de");
        assertThat(normalizer.normalize("text pkttest@test.de text")).contains("pkttest@test.de");
        assertThat(normalizer.normalize("text dottest@test.de text")).contains("dottest@test.de");
    }

    @Test
    public void replaceDot() {
        assertThat(normalizer.normalize("test(dot)foo@bar(punkt)com")).contains("test.foo@bar.com");
        assertThat(normalizer.normalize("test[dot]foo@bar[punkt]com")).contains("test.foo@bar.com");
        assertThat(normalizer.normalize("test{dot}foo@bar{punkt}com")).contains("test.foo@bar.com");
        assertThat(normalizer.normalize("test dot foo@bar punkt com")).contains("test.foo@bar.com");
        assertThat(normalizer.normalize("test pkt foo@bar pkt com")).contains("test.foo@bar.com");
        assertThat(normalizer.normalize("test@test#punkt#de")).contains("test@test.de");
        assertThat(normalizer.normalize("test@test_punkt_de")).contains("test@test.de");
    }

    @Test
    public void keepUnderscore() {
        String normalized = normalizer.normalize("test_foo@bar.com");
        assertThat(normalized).contains("test_foo@bar.com");
    }

    @Test
    public void findWithHtmlEncoding() {
        String normalized = normalizer.normalize("foo&#064;bar.com");
        assertThat(normalized).contains("foo@bar.com");
    }

    @Test
    public void keepDash() {
        String normalized = normalizer.normalize("foo-bar_foo@foo.com");
        assertThat(normalized).contains("foo-bar_foo@foo.com");
    }

    @Test
    public void allToLowerCase() {
        String normalized = normalizer.normalize("TestFooBar");
        assertThat(normalized).contains("testfoobar");
    }

}
