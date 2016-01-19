// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.mmseg.analysis;

import junit.framework.Assert;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-09-18
 */
public class NameAnalyzerTest {
    @Test
    public void test_analyzer() throws IOException {
        NameAnalyzer analyzer = new NameAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("", "李劲松");
        tokenStream.reset();
        CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
        StringBuilder sb = new StringBuilder();
        while(tokenStream.incrementToken()){
            sb.append(term).append("|");
        }

        Assert.assertEquals("李|劲|松|",sb.toString());
    }
}
