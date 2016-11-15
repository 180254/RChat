package pl.nn44.rchat.server.util;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.math.IntMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Random;

public class BigIdGenerator implements Iterator<String> {

    private static final Logger LOG = LoggerFactory.getLogger(BigIdGenerator.class);

    public static final int NUMBER_BASE = 32;
    public static final int BITS_PER_CHAR = IntMath.log2(NUMBER_BASE, RoundingMode.UNNECESSARY);

    private final Random random;
    private final int chars;
    private final int bits;

    private BigIdGenerator(Random random, int chars) {
        this.random = random;
        this.chars = chars;
        this.bits = chars * BITS_PER_CHAR;

        LOG.info("{} instance created: chars={}, bits={}.", getClass().getSimpleName(), chars, bits);
    }

    public static BigIdGenerator chars(Random random, int chars) {
        return new BigIdGenerator(random, chars);
    }

    public static BigIdGenerator bits(Random random, int bits) {
        int chars = (bits + BITS_PER_CHAR - 1) / BITS_PER_CHAR;
        return new BigIdGenerator(random, chars);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public int getChars() {
        return chars;
    }

    public int getBits() {
        return bits;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public String next() {
        String nextId = new BigInteger(bits, random).toString(NUMBER_BASE);
        return Strings.padStart(nextId, chars, '0');
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("chars", chars)
                .add("bits", bits)
                .toString();
    }
}
