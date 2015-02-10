// Copyright 2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.face.annotation;

import java.lang.annotation.*;

/**
 * 指定某一参数为服务器的id字段
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ServerId {
    String value() default "local";
}
