// Copyright 2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.model;

/**
 * 同步策略
 * @author jcai
 */
public enum SyncPolicy {
    /** 增量同步策略 **/
    Incremental,
    /** 全库同步策略 **/
    Full
}
