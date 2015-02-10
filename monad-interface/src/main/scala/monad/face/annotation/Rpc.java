// Copyright 2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.annotation;


import java.lang.annotation.*;


/**
 * rpc annotation
 * @author jcai
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Rpc {
    /**
     * 定义方法的id值，如果没有定义则采用类中方法的顺序，建议定义的值从10000开始，
     * 避免和内置的方法冲突
     * @return 方法序列
     */
    int methodId() default -1;

    /**
     * 定义的超时时间
     * @return 定义的超长时间
     */
    long timeoutInMillis() default 30000L;
    String mode() default "one";
    String merge() default "default";
    String waitMode() default "sync";
    boolean ignoreTimeout() default false;
}
