// Copyright 2012,2013,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model;

/**
 * 定义列的类型
 *
 * @author jcai
 * @version 0.1
 */
public enum ColumnType {
    //当做字符串处理
    String,//(new StringColumnType()),
    //Long 类型字段
    Long,//(new LongColumnType()),
    Int,//(new IntColumnType()),
    //日期类型，包含了字符串类型的日期
    Date,//(new DateColumnType()),
    //clob
    Clob;//(new ClobColumnType());

    /*
    private MonadColumnType<?> monadColumnType;

    ColumnType(MonadColumnType<?> monadColumnType) {
        this.monadColumnType = monadColumnType;
    }
    public MonadColumnType<?> getColumnType(){
        return this.monadColumnType;
    }
    */
}
