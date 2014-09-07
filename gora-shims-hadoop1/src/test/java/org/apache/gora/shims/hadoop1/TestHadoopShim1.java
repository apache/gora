package org.apache.gora.shims.hadoop1;

import org.apache.gora.shims.hadoop.HadoopShim;
import org.apache.gora.shims.hadoop.HadoopShimFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestHadoopShim1 {

    @Test
    public void testCorrectVersion() {
        HadoopShim shim = HadoopShimFactory.INSTANCE().getHadoopShim();

        assertSame(shim.getClass(), HadoopShim1.class);
    }

}
