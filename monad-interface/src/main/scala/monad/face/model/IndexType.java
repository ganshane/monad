// Copyright 2011,2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model;


/**
 * 索引的类型
 *
 * @author <a href="mailto:jun.tsai@gmail.com">Jun Tsai</a>
 * @version $Revision$
 * @since 0.1
 */
public enum IndexType {
    Keyword,//(Field.Store.NO, Field.Index.NOT_ANALYZED),
    Text,//(Field.Store.NO, Field.Index.ANALYZED),
    UnIndexed;//(Field.Store.NO, Field.Index.NO);

    /*
    //存储类型
    private Store storeType;
    //索引类型
    private Index indexType;

    IndexType(Field.Store storeType,Field.Index indexType) {
        this.storeType = storeType;
        this.indexType = indexType;
    }
    public Field.Store storeType(){
        return this.storeType;
    }
    public Field.Index indexType(){
        return this.indexType;
    }
    */
}
