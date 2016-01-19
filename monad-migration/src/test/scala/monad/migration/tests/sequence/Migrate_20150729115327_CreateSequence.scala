// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
/**
 * Copyright (c) 2015 Jun Tsai <jcai@ganshane.com>
 */
package monad.migration.tests.sequence

import monad.migration._

/**
 * test create sequence
 */
class Migrate_20150729115327_CreateSequence
    extends Migration {
  val sequenceName = "scala_migrations_sequence"

  def up(): Unit = {
    sequence(sequenceName)
  }

  def down() {
    dropSequence(sequenceName)
  }
}
