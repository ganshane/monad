// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.node.internal

import monad.core.internal.SlaveNoSQLServiceImplSupport
import monad.face.config.IndexConfigSupport
import monad.jni.services.gen.{NoSQLOptions, SlaveNoSQLSupport}

/**
 * implements NodeService
 */
class NodeNoSQLServiceImpl(config: IndexConfigSupport)
  extends SlaveNoSQLServiceImplSupport(config.noSql) {

  override protected def createNoSQLInstance(path: String, noSQLOption: NoSQLOptions): SlaveNoSQLSupport = {
    new SlaveNoSQLSupport(path, noSQLOption)
  }
}



