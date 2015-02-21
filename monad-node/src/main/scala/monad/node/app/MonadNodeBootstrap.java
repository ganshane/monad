// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
package monad.node.app;


import monad.support.services.AppBootstrap;

/**
 * monad启动程序
 *
 * @author jcai
 */
public class MonadNodeBootstrap extends AppBootstrap {
    public static void main(String[] args) throws Exception {
        start("monad.node.app.MonadNodeApp", args);
    }
}
