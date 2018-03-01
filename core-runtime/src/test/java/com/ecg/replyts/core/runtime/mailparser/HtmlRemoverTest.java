package com.ecg.replyts.core.runtime.mailparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class HtmlRemoverTest {

    @Test
    public void testOutOfBody() throws HtmlCleanupException {
        String resp = new HtmlRemover("Hi <body>again</body>").getPlaintext();
        Assert.assertEquals("Hi\nagain", resp);
    }

    @Test
    public void testBlockSpacing() throws HtmlCleanupException {
        String resp = new HtmlRemover("<body>Hi<p>again</p></body>").getPlaintext();
        Assert.assertEquals("Hi\nagain", resp);
    }

    @Test
    public void testInlineSpacing() throws HtmlCleanupException {
        String resp = new HtmlRemover("<body>Hi<b>again</b></body>").getPlaintext();
        Assert.assertEquals("Hiagain", resp);
    }

    @Test
    public void testHtmlEntities() throws HtmlCleanupException {
        String resp = new HtmlRemover("&#72;&#105;&#32;&#97;&#103;&#97;&#105;&#110;&#46;").getPlaintext();
        Assert.assertEquals("Hi again.", resp);
    }

    @Test
    public void testSpaceBeforeInline() throws Exception {
        String resp = new HtmlRemover("<body>Hello <span>World</span>").getPlaintext();
        Assert.assertEquals("Hello World", resp);
    }

    @Test
    public void testSpaceWithSpanWithoutFixEnabled() throws Exception {
        String resp = new HtmlRemover("<body>Hello <span rowtxt=\"rowmessage\">World</span>").getPlaintext();
        Assert.assertEquals("Hello World", resp);
    }

    @Test
    public void testSpaceWithSpanWithFixEnabled() throws Exception {
        HtmlRemover.IS_SPAN_FIX_ENABLED = true;
        String resp = new HtmlRemover("<body>Hello <span rowtxt=\"rowmessage\">World</span>").getPlaintext();
        HtmlRemover.IS_SPAN_FIX_ENABLED = false;

        Assert.assertEquals("World", resp);
    }

    @Test
    public void testNewLineWithinSpanTag() throws Exception {
        HtmlRemover.IS_SPAN_FIX_ENABLED = true;
        String resp = new HtmlRemover("<body>This is a <span rowtxt=\"rowmessage\">multi\nline\nmessage</span>").getPlaintext();
        HtmlRemover.IS_SPAN_FIX_ENABLED = false;

        Assert.assertEquals("multi\nline\nmessage", resp);
    }

    @Test
    public void testLineBreaks() throws HtmlCleanupException {
        String resp = new HtmlRemover("hello<br/>world").getPlaintext();
        Assert.assertEquals("hello\nworld", resp);
    }

    @Test
    public void testMaxLineBreaks() throws HtmlCleanupException {
        String resp = new HtmlRemover("<p>hello</p><br/><br/><p>world</p>").getPlaintext();
        Assert.assertEquals("hello\n\nworld", resp);
    }

    @Test
    public void testListBreaks() throws HtmlCleanupException {
        String resp = new HtmlRemover("<ol><li>Hello</li><li>World</li></ol>").getPlaintext();
        Assert.assertEquals("Hello\nWorld", resp);
    }

    @Test
    public void testScriptRemoval() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test2.html");
        InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
        String resp = new HtmlRemover(isr).getPlaintext();

        Assert.assertTrue("Content of style tag should have been removed", resp.indexOf("body, div, dl") < 0);
    }

    @Test
    public void testBlockLinebreak() throws Exception {
        String input = new HtmlRemover("<div>Hello</div>World").getPlaintext();
        Assert.assertEquals("Hello\nWorld", input);
    }

    @Test
    public void testNewLines() throws Exception {
        String input = new HtmlRemover("Hello\n   \t   \n        \t\t\t\n        World").getPlaintext();
        Assert.assertEquals("Hello World", input);
    }

    @Test
    public void testRemoveDoubleBlockIndent() throws Exception {
        String input = new HtmlRemover("<div>Hello</div><div>World</div>").getPlaintext();
        Assert.assertEquals("Hello\nWorld", input);
    }

    @Test
    public void testFlushDataEnd() throws Exception {
        String input = new HtmlRemover("<div>Hello</div>World").getPlaintext();
        Assert.assertEquals("Hello\nWorld", input);
    }

    @Test
    public void testCommentRemovement() throws Exception {
        String input = new HtmlRemover("Hello<!-- YOU STUPID!!! -->World").getPlaintext();
        Assert.assertEquals("HelloWorld", input);
    }

    @Test
    public void stripNonBreakingSpace() throws Exception {
        String input = "Hello&nbsp;World";

        String inputEsc = new HtmlRemover(input).getPlaintext();
        Assert.assertEquals("Hello World", inputEsc);
    }

    @Test
    public void keepSeperateWordsBoundedByInlineTagsSeperate() throws Exception {
        assertEquals("hello world", new HtmlRemover("<b>hello</b> world").getPlaintext());
    }

    @Test
    public void testStripBigDom() throws Exception {
        Charset UTF8 = Charset.forName("UTF-8");
        InputStream html = getClass().getResourceAsStream("/largeHtml.html");
        String resp = new HtmlRemover(new InputStreamReader(html, UTF8)).getPlaintext();
        Assert.assertTrue(resp.length() > 1e3);
    }
}
