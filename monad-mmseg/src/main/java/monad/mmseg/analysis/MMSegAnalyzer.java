package monad.mmseg.analysis;

import monad.mmseg.Dictionary;
import monad.mmseg.MaxWordSeg;
import monad.mmseg.Seg;
import org.apache.lucene.analysis.Analyzer;

import java.io.File;

/**
 * 默认使用 max-word
 *
 * @author chenlb
 * @see {@link SimpleAnalyzer}, {@link ComplexAnalyzer}, {@link MaxWordAnalyzer}
 */
public class MMSegAnalyzer extends Analyzer {

    protected Dictionary dic;

    /**
     * @see Dictionary#getInstance()
     */
    public MMSegAnalyzer() {
        dic = Dictionary.getInstance();
    }

    /**
     * @param path 词库路径
     * @see Dictionary#getInstance(String)
     */
    public MMSegAnalyzer(String path) {
        dic = Dictionary.getInstance(path);
    }

    /**
     * @param path 词库目录
     * @see Dictionary#getInstance(File)
     */
    public MMSegAnalyzer(File path) {
        dic = Dictionary.getInstance(path);
    }

    public MMSegAnalyzer(Dictionary dic) {
        super();
        this.dic = dic;
    }

    protected Seg newSeg() {
        return new MaxWordSeg(dic);
    }

    public Dictionary getDict() {
        return dic;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new MMSegTokenizer(newSeg()));
    }
}
