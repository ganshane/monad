// Copyright 2012,2013,2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.cloud.app;


import stark.utils.services.AppBootstrap;

/**
 * monad启动程序
 *
 * @author jcai
 */
public class MonadCloudBootstrap extends AppBootstrap {
    public static void main(String[] args) throws Exception {
        start("monad.cloud.app.MonadCloudApp", args);
    }
}
