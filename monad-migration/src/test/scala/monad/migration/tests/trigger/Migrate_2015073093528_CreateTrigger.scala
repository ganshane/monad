/**
 * Copyright (c) 2015 Jun Tsai <jcai@ganshane.com>
 */
package monad.migration.tests.trigger

import monad.migration._

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
