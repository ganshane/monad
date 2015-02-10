// Copyright 2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.annotation;


import java.lang.annotation.*;


/**
 * rpc service annotation
 * @author jcai
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RpcServiceConfig {
    /**
     * 每个server需要打开的channel个数
     * @return 需要打开的个数
     */
    int channelsPerServer() default 1;

    /**
     * 远程执行过程中buffer的大小，
     * 如果超过此buffer将进行等待
     * @return buffer的大小
     */
    int taskBufferSize() default 1;
}
