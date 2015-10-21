package monad.mmseg.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;

/**
 * 针对姓名的分词算法
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-09-18
 */
public class NameAnalyzer extends Analyzer{
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new NGramTokenizer(1,1);
        return new Analyzer.TokenStreamComponents(source, new NGramTokenFilter(source));
    }
}
