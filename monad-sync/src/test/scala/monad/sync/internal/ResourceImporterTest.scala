// Copyright 2012,2013 The EGF IT Software Department.
// site: http://www.ganshane.com
/*
 * Copyright 2012 The EGF IT Software Department.
 */
package monad.sync.internal

import java.io.{BufferedReader, InputStreamReader}
import java.sql.Connection

import monad.face.model.ResourceDefinition
import monad.jni.JniLoader
import monad.support.services.XmlLoader
import org.apache.tapestry5.ioc.internal.util.InternalUtils
import org.junit.Test
import org.slf4j.LoggerFactory

/**
 *
 * @author jcai
 */
class ResourceImporterTest {
  private val logger = LoggerFactory getLogger getClass

  @Test
  def test_scala {
    val c1 = Array("adsf", "fdsa")
    val c2 = Array("1", null)
    for ((a, b) <- c1 zip c2 if b != null) {
      println(a + b)
    }

  }

  def test_import_resource {
    JniLoader.loadJniLibrary("support")
    val rd = XmlLoader.parseXML[ResourceDefinition](getClass.getResourceAsStream("/czrk.xml"), None)

    /*
    val config = new MonadSyncConfig
    config.sync.noSql.cache = 1
    config.sync.noSql.path = "target/test_importer"
    config.sync.noSql.writeBuffer = 1
    val importer = new ResourceImporter(rd,operator,null,null,0,config)
    importer.buildConnection
    loadTestData(importer.conn)
    importer.init
    importer.importData

    importer.importData
    importer.shutdown

    //verify(operator,times(3)).importData(Matchers.anyObject(),Matchers.anyObject(),null,0)
    */
  }

  private def loadTestData(conn: Connection) {
    val in = new BufferedReader(new InputStreamReader(getClass.getResourceAsStream("/czrk.sql"), "UTF-8"))
    Stream.continually(in.readLine).takeWhile(_ != null).filter(InternalUtils.isNonBlank).foreach { sql =>
      if (InternalUtils.isNonBlank(sql)) {
        logger.debug("[Test-DB]" + sql)
        JdbcDatabase.execute(sql)(conn)
      }
    }
    JdbcDatabase.use(autoCommit = true)(_.commit())(conn)
  }
}
