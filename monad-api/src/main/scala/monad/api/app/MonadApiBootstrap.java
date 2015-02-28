// Copyright 2012,2013,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.api.app;


import monad.support.services.AppBootstrap;

/**
 * monad启动程序
 *
 * @author jcai
 */
public class MonadApiBootstrap extends AppBootstrap {
    public static void main(String[] args) throws Exception {
        start("monad.api.app.MonadApiApp", args);
    }
}
