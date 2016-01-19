// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.mmseg.example;

import monad.mmseg.MaxWordSeg;
import monad.mmseg.Seg;

import java.io.IOException;

public class MaxWord extends Complex {

    public static void main(String[] args) throws IOException {
        new MaxWord().run(args);
    }

    protected Seg getSeg() {

        return new MaxWordSeg(dic);
    }
}
