package monad.mmseg.rule;

import monad.mmseg.Chunk;

/**
 * Largest Average Word Length.<p/>
 * <p/>
 * 长度(Length)/词数
 *
 * @author chenlb 2009-3-16 上午11:28:21
 * @see http://technology.chtsai.org/mmseg/
 */
public class LargestAvgLenRule extends Rule {

    private double largestAvgLen;

    @Override
    public void addChunk(Chunk chunk) {
        if (chunk.getAvgLen() >= largestAvgLen) {
            largestAvgLen = chunk.getAvgLen();
            super.addChunk(chunk);
        }
    }

    @Override
    protected boolean isRemove(Chunk chunk) {
        return chunk.getAvgLen() < largestAvgLen;
    }

    @Override
    public void reset() {
        largestAvgLen = 0;
        super.reset();
    }

}
