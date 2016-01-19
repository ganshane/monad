// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/**
 * Copyright (c) 2015 Jun Tsai <jcai@ganshane.com>
 */
package monad.migration.tests.trigger

import java.sql.Connection

import monad.migration._
import org.h2.api.Trigger

/**
 * test create sequence
 */
class Migrate_2015073093528_CreateTrigger
    extends Migration {
  val tableName = "scala_migrations_trigger"

  def up(): Unit = {
    createTable(tableName){t=>
      t.varchar("col",Limit(200))
    }
    databaseVendor match {
      case Oracle =>
        addTrigger(tableName,"test_trigger",Before,Update,When("NEW.User_ID is NULL")){
          "NULL"
        }
      case H2 =>
        addTrigger(tableName,"test_trigger",Before,Update,ForEachRow){
          "CALL \"%s\" ".format(classOf[MyTrigger].getName)
          //"$$org.h2.api.Trigger create() { return new MyTrigger(\"constructorParam\"); } $$;"
        }
      case other=>
    }
  }

  def down(): Unit = {
    databaseVendor match{
      case Oracle =>
        dropTrigger("test_trigger")
      case other=>
    }
    dropTable(tableName)
  }
}
class MyTrigger extends Trigger{
  override def fire(conn: Connection, oldRow: Array[AnyRef], newRow: Array[AnyRef]): Unit = {

  }

  override def init(conn: Connection, schemaName: String, triggerName: String, tableName: String, before: Boolean, `type`: Int): Unit = {

  }

  override def remove(): Unit = {}

  override def close(): Unit = {}
}
