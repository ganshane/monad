// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.mmseg.analysis;

import monad.mmseg.Dictionary;
import monad.mmseg.MaxWordSeg;
import monad.mmseg.Seg;

import java.io.File;


/**
 * 最多分词方式.
 *
 * @author chenlb 2009-4-6 下午08:43:46
 */
public class MaxWordAnalyzer extends MMSegAnalyzer {

    public MaxWordAnalyzer() {
        super();
    }

    public MaxWordAnalyzer(String path) {
        super(path);
    }

    public MaxWordAnalyzer(Dictionary dic) {
        super(dic);
    }

    public MaxWordAnalyzer(File path) {
        super(path);
    }

    protected Seg newSeg() {
        return new MaxWordSeg(dic);
    }
}
