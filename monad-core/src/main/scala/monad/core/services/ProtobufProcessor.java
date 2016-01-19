// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.services;

import com.google.protobuf.GeneratedMessage;

/**
 * protobuf processor
 *
 * @author jcai
 */
public interface ProtobufProcessor<T extends GeneratedMessage, R extends GeneratedMessage> {
    /**
     * 对传入的protobuf请求进行处理
     *
     * @param request protobuf请求
     * @return 处理结果
     */
    public R process(T request);
}
