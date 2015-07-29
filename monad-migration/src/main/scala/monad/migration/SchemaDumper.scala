package monad.migration

/**
 * schema dumper
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-28
 */
object SchemaDumper {
  def main(args:Array[String]): Unit ={
    val driver = "oracle.jdbc.driver.OracleDriver"
    val url = System.getProperty("dump.jdbc.url")
    val user = System.getProperty("dump.jdbc.user")
    val pass = System.getProperty("dump.jdbc.pass")
    val schema = System.getProperty("dump.jdbc.schema")

    val vendor = Vendor.forDriver(driver)
    val connectionBuilder = new ConnectionBuilder(url, user, pass)
    val databaseAdapter = DatabaseAdapter.forVendor(vendor,Option(schema))
    implicit val sb = new StringBuilder
    val migrator = new Migrator(connectionBuilder,databaseAdapter)
    migrator.tables()
      .filter{x=> x equals "GAFIS_GATHER_FINGER"}
      .take(2).foreach{x=>println(x);migrator.dumpTable(x)}
    migrator.sequences().foreach(migrator.dumpSequence)
    println(sb)
  }
}
