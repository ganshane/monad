package monad.mmseg.example;

import monad.mmseg.Seg;
import monad.mmseg.SimpleSeg;

import java.io.IOException;

/**
 * @author chenlb 2009-3-14 上午12:38:40
 */
public class Simple extends Complex {

    public static void main(String[] args) throws IOException {
        new Simple().run(args);
    }

    protected Seg getSeg() {

        return new SimpleSeg(dic);
    }

}
