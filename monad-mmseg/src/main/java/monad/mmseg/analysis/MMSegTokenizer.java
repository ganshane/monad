// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.mmseg.analysis;

import monad.mmseg.MMSeg;
import monad.mmseg.Seg;
import monad.mmseg.Word;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class MMSegTokenizer extends Tokenizer {

    private final Seg seg;
    private MMSeg mmSeg;
    private CharTermAttribute termAtt;
    private OffsetAttribute offsetAtt;
    private TypeAttribute typeAtt;

    public MMSegTokenizer(Seg seg) {
        this.seg = seg;

        termAtt = addAttribute(CharTermAttribute.class);
        offsetAtt = addAttribute(OffsetAttribute.class);
        typeAtt = addAttribute(TypeAttribute.class);
    }

    public void reset() throws IOException {
        super.reset();
        //lucene 4.0
        //org.apache.lucene.analysis.Tokenizer.setReader(Reader)
        //setReader 自动被调用, input 自动被设置。
        if (mmSeg == null) {
            mmSeg = new MMSeg(input, seg);
        } else {
            mmSeg.reset(input);
        }
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        Word word = mmSeg.next();
        if (word != null) {
            termAtt.copyBuffer(word.getSen(), word.getWordOffset(), word.getLength());
            offsetAtt.setOffset(word.getStartOffset(), word.getEndOffset());
            typeAtt.setType(word.getType());
            return true;
        } else {
            return false;
        }
    }
}
