// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.core.internal

import java.io.{File, IOException}

import monad.core.services.MonadCoreErrorCode
import monad.support.services.{LoggerSupport, MonadException, MonadUtils}
import org.apache.commons.io.FileUtils

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * 实现本地存储用的KV数据库(NoSQL),主要存储本地化的设置.
 * 注意：  不可以频繁使用，
 * 不能用来承载业务数据
 */
class LocalSimpleStore(dir: String) extends LoggerSupport {
  //global store object
  private val _vs = new VersionedStore

  /**
   * get value from kv database
   * @param key key
   * @tparam T value type
   * @return value
   */
  def get[T](key: Any): Option[T] = {
    snapshot.get(key).asInstanceOf[Option[T]]
  }

  /**
   * get kv database snapshot
   * @return local database
   */
  private def snapshot: mutable.Map[Any, Any] = {

    @tailrec
    def internalSnapshot(seq: Int, e: Option[Throwable]): mutable.Map[Any, Any] = {
      if (seq > 0) {
        val latestPath = _vs.mostRecentVersionPath
        if (latestPath.isEmpty) return mutable.Map.empty[Any, Any]
        try {
          val bytes = FileUtils.readFileToByteArray(new File(latestPath.get))
          MonadUtils.deserialize(bytes).asInstanceOf[mutable.Map[Any, Any]]
        } catch {
          case ex: IOException =>
            internalSnapshot(seq - 1, Some(ex))
        }
      } else {
        e match {
          case Some(ex) =>
            throw MonadException.wrap(ex, MonadCoreErrorCode.FAIL_TO_GET_LOCAL_KV_SNAPSHOT)
          case None =>
            throw new MonadException("fail to snapshot", MonadCoreErrorCode.FAIL_TO_GET_LOCAL_KV_SNAPSHOT)
        }
      }
    }

    internalSnapshot(10, None)
  }

  /**
   * put data to kv database
   * @param key key of data
   * @param value value
   */
  def put(key: Any, value: Any) {
    put(key, value, cleanup = true)
  }

  /**
   * put data
   * @param key key of data
   * @param value value object
   * @param cleanup whether cleanup database
   */
  def put(key: Any, value: Any, cleanup: Boolean) {
    val curr: mutable.Map[Any, Any] = snapshot
    curr.put(key, value)
    persist(curr, cleanup)
  }

  /**
   * remove data with key given
   * @param key key be removed
   */
  def remove(key: Any) {
    remove(key, cleanup = true)
  }

  def remove(key: Any, cleanup: Boolean) {
    val curr: mutable.Map[Any, Any] = snapshot
    curr.remove(key)
    persist(curr, cleanup)
  }

  private def persist(value: mutable.Map[Any, Any], cleanup: Boolean) {
    val toWrite: Array[Byte] = MonadUtils.serialize(value)
    val newPath: String = _vs.createVersion
    FileUtils.writeByteArrayToFile(new File(newPath), toWrite)
    _vs.succeedVersion(newPath)
    //仅仅保存4个版本
    if (cleanup) _vs.cleanup(4)
  }

  def cleanup(keepVersions: Int) {
    _vs.cleanup(keepVersions)
  }

  private class VersionedStore {
    private final val FINISHED_VERSION_SUFFIX: String = ".version"
    assert(dir != null, "kv database directory is null!")
    private val _root: String = dir
    mkdirs(_root)

    def getRoot: String = {
      _root
    }

    def mostRecentVersionPath: Option[String] = {
      mostRecentVersion.map(versionPath)
    }

    def mostRecentVersion: Option[Long] = {
      getAllVersions.headOption
    }

    def mostRecentVersionPath(maxVersion: Long): Option[String] = {
      val versionOpt = mostRecentVersion(maxVersion)
      versionOpt.map(versionPath)
    }

    def mostRecentVersion(maxVersion: Long): Option[Long] = {
      getAllVersions.takeWhile(_ <= maxVersion).headOption
    }

    /**
     * Sorted from most recent to oldest
     */
    def getAllVersions: List[Long] = {
      listDir(_root).
        filter(_.endsWith(FINISHED_VERSION_SUFFIX)).
        map(validateAndGetVersion).
        sortWith(_.compareTo(_) > 0)
    }

    private def listDir(dir: String): List[String] = {
      val contents = new File(dir).listFiles
      if (contents != null) {
        return contents.map(_.getAbsolutePath).toList
      }
      List[String]()
    }

    private def validateAndGetVersion(path: String): Long = {
      val v: Long = parseVersion(path)
      if (v <= 0) throw new MonadException(path + " is not a valid version", MonadCoreErrorCode.INVALID_LOCAL_VERSION)
      v
    }

    private def parseVersion(path: String): Long = {
      var name: String = new File(path).getName
      if (name.endsWith(FINISHED_VERSION_SUFFIX)) {
        name = name.substring(0, name.length - FINISHED_VERSION_SUFFIX.length)
      }
      try {
        java.lang.Long.parseLong(name)
      }
      catch {
        case e: NumberFormatException =>
          -1L

      }
    }

    def versionPath(version: Long): String = {
      new File(_root, "" + version).getAbsolutePath
    }

    def createVersion: String = {
      val mostRecent = mostRecentVersion
      val version = mostRecent match {
        case Some(x) =>
          x + 1
        case None =>
          System.currentTimeMillis()
      }
      createVersion(version)
    }

    def createVersion(version: Long): String = {
      val ret: String = versionPath(version)
      if (getAllVersions.contains(version))
        throw new MonadException(
          "Version already exists or data already exists",
          MonadCoreErrorCode.VERSION_EXISTS_IN_KV)
      else ret
    }

    def failVersion(path: String) {
      deleteVersion(validateAndGetVersion(path))
    }

    def succeedVersion(path: String) {
      val version: Long = validateAndGetVersion(path)
      createNewFile(tokenPath(version))
    }

    private def createNewFile(path: String) {
      new File(path).createNewFile
    }

    def cleanup() {
      cleanup(-1)
    }

    def cleanup(versionsToKeep: Int) {
      var versions: List[Long] = getAllVersions
      versions = versions.take(math.min(versions.size, versionsToKeep))

      listDir(_root).foreach { x =>
        val v: Long = parseVersion(x)
        if (v > 0 && !versions.contains(v)) {
          deleteVersion(v)
        }
      }
    }

    def deleteVersion(version: Long) {
      val versionFile: File = new File(versionPath(version))
      val tokenFile: File = new File(tokenPath(version))
      if (versionFile.exists) {
        FileUtils.forceDelete(versionFile)
      }
      if (tokenFile.exists) {
        FileUtils.forceDelete(tokenFile)
      }
    }

    private def tokenPath(version: Long): String = {
      new File(_root, "" + version + FINISHED_VERSION_SUFFIX).getAbsolutePath
    }

    private def mkdirs(path: String) {
      new File(path).mkdirs
    }

  }

}


