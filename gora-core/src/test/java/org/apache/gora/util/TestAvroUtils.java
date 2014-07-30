package org.apache.gora.util;

import java.nio.ByteBuffer;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.WebPage;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class TestAvroUtils {

    @Test
    public void testDeepClonePersistent() throws Exception {
        CharSequence url = new Utf8("http://gora.apache.org/");
        WebPage.Builder builder = WebPage.newBuilder()
            .setUrl(url)
            .setContent(ByteBuffer.wrap("Gora".getBytes("UTF-8")));
        WebPage webPage = builder.build();
        WebPage clonedWebPage = AvroUtils.deepClonePersistent(webPage);
        assertThat(clonedWebPage, is(notNullValue()));
        assertThat(clonedWebPage.getUrl(), is(equalTo(url)));
        assertThat(clonedWebPage.getContent(), is(notNullValue()));
        String clonedWebPageContent = new String(clonedWebPage.getContent().array(), "UTF-8");
        assertThat(clonedWebPageContent, is(equalTo("Gora")));
    }

}
