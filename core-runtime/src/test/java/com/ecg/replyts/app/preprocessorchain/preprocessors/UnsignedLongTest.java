package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.util.UnsignedLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnsignedLongTest {
    @Test
    public void canConvertToBase30() {
        assertEquals("0=0", "0", UnsignedLong.fromLong(0L).toBase30());
        assertEquals("1=1", "1", UnsignedLong.fromLong(1L).toBase30());
        assertEquals("2=2", "2", UnsignedLong.fromLong(2L).toBase30());
        assertEquals("3=3", "3", UnsignedLong.fromLong(3L).toBase30());
        assertEquals("4=4", "4", UnsignedLong.fromLong(4L).toBase30());
        assertEquals("5=5", "5", UnsignedLong.fromLong(5L).toBase30());
        assertEquals("6=6", "6", UnsignedLong.fromLong(6L).toBase30());
        assertEquals("7=7", "7", UnsignedLong.fromLong(7L).toBase30());
        assertEquals("8=8", "8", UnsignedLong.fromLong(8L).toBase30());
        assertEquals("9=9", "9", UnsignedLong.fromLong(9L).toBase30());
        assertEquals("10=b", "b", UnsignedLong.fromLong(10L).toBase30());
        assertEquals("11=c", "c", UnsignedLong.fromLong(11L).toBase30());
        assertEquals("12=d", "d", UnsignedLong.fromLong(12L).toBase30());
        assertEquals("13=f", "f", UnsignedLong.fromLong(13L).toBase30());
        assertEquals("14=g", "g", UnsignedLong.fromLong(14L).toBase30());
        assertEquals("15=h", "h", UnsignedLong.fromLong(15L).toBase30());
        assertEquals("16=j", "j", UnsignedLong.fromLong(16L).toBase30());
        assertEquals("17=k", "k", UnsignedLong.fromLong(17L).toBase30());
        assertEquals("18=l", "l", UnsignedLong.fromLong(18L).toBase30());
        assertEquals("19=m", "m", UnsignedLong.fromLong(19L).toBase30());
        assertEquals("20=n", "n", UnsignedLong.fromLong(20L).toBase30());
        assertEquals("21=p", "p", UnsignedLong.fromLong(21L).toBase30());
        assertEquals("22=q", "q", UnsignedLong.fromLong(22L).toBase30());
        assertEquals("23=r", "r", UnsignedLong.fromLong(23L).toBase30());
        assertEquals("24=s", "s", UnsignedLong.fromLong(24L).toBase30());
        assertEquals("25=t", "t", UnsignedLong.fromLong(25L).toBase30());
        assertEquals("26=v", "v", UnsignedLong.fromLong(26L).toBase30());
        assertEquals("27=w", "w", UnsignedLong.fromLong(27L).toBase30());
        assertEquals("28=x", "x", UnsignedLong.fromLong(28L).toBase30());
        assertEquals("29=z", "z", UnsignedLong.fromLong(29L).toBase30());
        assertEquals("30=10", "10", UnsignedLong.fromLong(30L).toBase30());
    }

    @Test
    public void canConvertToBase30BeyondJavaSignedLong() {
        // Treated as unsigned, Long.MIN_VALUE is equivalent to Long.MAX_VALUE + 1
        assertEquals("kbmttcd1hd207", UnsignedLong.fromLong(Long.MAX_VALUE).toBase30());
        assertEquals("kbmttcd1hd208", UnsignedLong.fromLong(Long.MIN_VALUE).toBase30());

        assertEquals("14p9pnqs30s40h", UnsignedLong.fromLong(-1).toBase30());
        assertEquals("14p9pnqs30s40g", UnsignedLong.fromLong(-2).toBase30());
    }
}