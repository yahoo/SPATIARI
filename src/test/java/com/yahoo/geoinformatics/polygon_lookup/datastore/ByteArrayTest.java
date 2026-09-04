package com.yahoo.geoinformatics.polygon_lookup.datastore;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ByteArrayTest {

    @Test
    public void testWriteAndRead() {
        byte[] buffer = new byte[16];
        int value1 = 101;
        int value2 = -23;
        int value3 = 37578515;
        int value4 = -121973703;

        ByteArray.writeInt(buffer, 0, value1);
        Assert.assertEquals(ByteArray.readInt(buffer, 0), value1);

        ByteArray.writeInt(buffer, 4, value2);
        ByteArray.writeInt(buffer, 8, value3);
        ByteArray.writeInt(buffer, 12, value4);

        Assert.assertEquals(ByteArray.readInt(buffer, 4), value2);
        Assert.assertEquals(ByteArray.readInt(buffer, 8), value3);
        Assert.assertEquals(ByteArray.readInt(buffer, 12), value4);
    }

}
