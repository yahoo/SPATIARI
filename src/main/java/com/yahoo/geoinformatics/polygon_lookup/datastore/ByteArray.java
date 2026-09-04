package com.yahoo.geoinformatics.polygon_lookup.datastore;

/**
 * Utility class for read to and write from byte buffer
 *
 * @author koushikm
 */
public class ByteArray {

    /**
     * Read an integer from the byte buffer at the given offset.
     *
     * @param buffer Array to read from
     * @param offset Offset to read at
     * @return data
     */
    public static int readInt(byte[] buffer, int offset) {
        int b0 = buffer[offset + 0] & 0xFF;
        int b1 = buffer[offset + 1] & 0xFF;
        int b2 = buffer[offset + 2] & 0xFF;
        int b3 = buffer[offset + 3] & 0xFF;
        return ((b0 << 24) + (b1 << 16) + (b2 << 8) + (b3 << 0));
    }

    /**
     * Write an integer to the byte buffer at the given offset.
     *
     * @param buffer Array to write to
     * @param offset Offset to write to
     * @param value  data
     */
    public static void writeInt(byte[] buffer, int offset, int value) {
        buffer[offset + 0] = (byte) (value >>> 24);
        buffer[offset + 1] = (byte) (value >>> 16);
        buffer[offset + 2] = (byte) (value >>> 8);
        buffer[offset + 3] = (byte) (value >>> 0);
    }
}
