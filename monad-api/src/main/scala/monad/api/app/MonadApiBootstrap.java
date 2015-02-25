// Copyright 2012,2013 The EGF IT Software Department.
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
