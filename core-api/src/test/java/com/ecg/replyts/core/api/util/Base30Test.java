package com.ecg.replyts.core.api.util;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class Base30Test {
    @Test
    public void positiveLongsConvertProperly() {
        assertEquals("0=0", "0", Base30.convert(0L));
        assertEquals("1=1", "1", Base30.convert(1L));
        assertEquals("2=2", "2", Base30.convert(2L));
        assertEquals("3=3", "3", Base30.convert(3L));
        assertEquals("4=4", "4", Base30.convert(4L));
        assertEquals("5=5", "5", Base30.convert(5L));
        assertEquals("6=6", "6", Base30.convert(6L));
        assertEquals("7=7", "7", Base30.convert(7L));
        assertEquals("8=8", "8", Base30.convert(8L));
        assertEquals("9=9", "9", Base30.convert(9L));
        assertEquals("10=b", "b", Base30.convert(10L));
        assertEquals("11=c", "c", Base30.convert(11L));
        assertEquals("12=d", "d", Base30.convert(12L));
        assertEquals("13=f", "f", Base30.convert(13L));
        assertEquals("14=g", "g", Base30.convert(14L));
        assertEquals("15=h", "h", Base30.convert(15L));
        assertEquals("16=j", "j", Base30.convert(16L));
        assertEquals("17=k", "k", Base30.convert(17L));
        assertEquals("18=l", "l", Base30.convert(18L));
        assertEquals("19=m", "m", Base30.convert(19L));
        assertEquals("20=n", "n", Base30.convert(20L));
        assertEquals("21=p", "p", Base30.convert(21L));
        assertEquals("22=q", "q", Base30.convert(22L));
        assertEquals("23=r", "r", Base30.convert(23L));
        assertEquals("24=s", "s", Base30.convert(24L));
        assertEquals("25=t", "t", Base30.convert(25L));
        assertEquals("26=v", "v", Base30.convert(26L));
        assertEquals("27=w", "w", Base30.convert(27L));
        assertEquals("28=x", "x", Base30.convert(28L));
        assertEquals("29=z", "z", Base30.convert(29L));
        assertEquals("30=10", "10", Base30.convert(30L));

        assertEquals("31=11", "11", Base30.convert(31L));
        assertEquals("60=20", "20", Base30.convert(60L));
        assertEquals("899=zz", "zz", Base30.convert(899));
        assertEquals("900=100", "100", Base30.convert(900L));
        assertEquals("901=101", "101", Base30.convert(901L));
        assertEquals("kbmttcd1hd200", Base30.convert(Long.MAX_VALUE - 7));
        assertEquals("kbmttcd1hd207", Base30.convert(Long.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesToCalculateNegativeLongs() {
        Base30.convert(-1L);
        Assert.fail("base 30 of -1 should have thrown an exception.");
    }

    @Test
    public void positiveBigIntegersConvertProperly() {
        assertEquals("0=0", "0", Base30.convert(BigInteger.valueOf(0L)));
        assertEquals("1=1", "1", Base30.convert(BigInteger.valueOf(1L)));
        assertEquals("2=2", "2", Base30.convert(BigInteger.valueOf(2L)));
        assertEquals("3=3", "3", Base30.convert(BigInteger.valueOf(3L)));
        assertEquals("4=4", "4", Base30.convert(BigInteger.valueOf(4L)));
        assertEquals("5=5", "5", Base30.convert(BigInteger.valueOf(5L)));
        assertEquals("6=6", "6", Base30.convert(BigInteger.valueOf(6L)));
        assertEquals("7=7", "7", Base30.convert(BigInteger.valueOf(7L)));
        assertEquals("8=8", "8", Base30.convert(BigInteger.valueOf(8L)));
        assertEquals("9=9", "9", Base30.convert(BigInteger.valueOf(9L)));
        assertEquals("10=b", "b", Base30.convert(BigInteger.valueOf(10L)));
        assertEquals("11=c", "c", Base30.convert(BigInteger.valueOf(11L)));
        assertEquals("12=d", "d", Base30.convert(BigInteger.valueOf(12L)));
        assertEquals("13=f", "f", Base30.convert(BigInteger.valueOf(13L)));
        assertEquals("14=g", "g", Base30.convert(BigInteger.valueOf(14L)));
        assertEquals("15=h", "h", Base30.convert(BigInteger.valueOf(15L)));
        assertEquals("16=j", "j", Base30.convert(BigInteger.valueOf(16L)));
        assertEquals("17=k", "k", Base30.convert(BigInteger.valueOf(17L)));
        assertEquals("18=l", "l", Base30.convert(BigInteger.valueOf(18L)));
        assertEquals("19=m", "m", Base30.convert(BigInteger.valueOf(19L)));
        assertEquals("20=n", "n", Base30.convert(BigInteger.valueOf(20L)));
        assertEquals("21=p", "p", Base30.convert(BigInteger.valueOf(21L)));
        assertEquals("22=q", "q", Base30.convert(BigInteger.valueOf(22L)));
        assertEquals("23=r", "r", Base30.convert(BigInteger.valueOf(23L)));
        assertEquals("24=s", "s", Base30.convert(BigInteger.valueOf(24L)));
        assertEquals("25=t", "t", Base30.convert(BigInteger.valueOf(25L)));
        assertEquals("26=v", "v", Base30.convert(BigInteger.valueOf(26L)));
        assertEquals("27=w", "w", Base30.convert(BigInteger.valueOf(27L)));
        assertEquals("28=x", "x", Base30.convert(BigInteger.valueOf(28L)));
        assertEquals("29=z", "z", Base30.convert(BigInteger.valueOf(29L)));
        assertEquals("30=10", "10", Base30.convert(BigInteger.valueOf(30L)));

        assertEquals("31=11", "11", Base30.convert(BigInteger.valueOf(31L)));
        assertEquals("60=20", "20", Base30.convert(BigInteger.valueOf(60L)));
        assertEquals("899=zz", "zz", Base30.convert(BigInteger.valueOf(899)));
        assertEquals("900=100", "100", Base30.convert(BigInteger.valueOf(900L)));
        assertEquals("901=101", "101", Base30.convert(BigInteger.valueOf(901L)));
        assertEquals("kbmttcd1hd200", Base30.convert(BigInteger.valueOf(Long.MAX_VALUE - 7)));
        assertEquals("kbmttcd1hd207", Base30.convert(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesToCalculateNegativeBigIntegers() {
        Base30.convert(BigInteger.valueOf(-1L));
        Assert.fail("base 30 of -1 should have thrown an exception.");
    }
}