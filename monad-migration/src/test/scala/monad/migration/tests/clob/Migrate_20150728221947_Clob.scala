/**
 * Copyright (c) 2015 Jun Tsai <jcai@ganshane.com>
 */
package monad.migration.tests.clob

import monad.migration._

class Migrate_20150728221947_Clob
    extends Migration {
  val tableName = "scala_migrations_clob"

  def up() {
    createTable(tableName,Comment("表1")) { t =>
      t.varchar("name", Unique, Limit(127), NotNull,Comment("列1"))
      t.clob("clob_field")
    }

    commentTable(tableName,"修改表1")
    commentColumn(tableName,"name","列1的注释")

    alterColumn(tableName,"name",BigintType,Comment("修改列1的注释"))
  }

  def down() {
    dropTable(tableName)
  }
}
