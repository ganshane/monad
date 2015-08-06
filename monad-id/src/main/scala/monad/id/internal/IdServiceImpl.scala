package monad.id.internal

import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

import monad.face.services.DataTypeUtils
import monad.id.config.MonadIdConfig
import monad.id.services.IdService
import monad.jni.services.gen.{NoSQLOptions, NoSQLSupport}
import monad.protocol.internal.InternalIdProto.IdCategory
import monad.support.services.{LoggerSupport, ServiceUtils}
import org.apache.commons.io.FileUtils
import org.apache.tapestry5.ioc.services.RegistryShutdownHub

import scala.collection.mutable

/**
 * implements id service
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-07-13
 */
class IdServiceImpl(config:MonadIdConfig) extends IdService with LoggerSupport{
  private val ids = new mutable.HashMap[IdCategory,NoSQLCategoryIdService]()
  @PostConstruct
  def initService(hub:RegistryShutdownHub): Unit ={
    IdCategory.values().foreach{category =>
      val categoryIdService = new NoSQLCategoryIdService(category)
      ServiceUtils.runInNoThrow{
        categoryIdService.start()
        ids.put(category,categoryIdService)
      }
    }
    if(hub != null){
      hub.addRegistryShutdownListener(new Runnable {
        override def run(): Unit = {
          shutdown()
        }
      })
    }
  }
  private[internal] def shutdown(): Unit ={
    ids.values.foreach(_.shutdown())
  }
  /**
   * 得到一个对象的序列值
   * @param category 类别
   * @param idLabel 对象字符串
   * @return 对象的序列
   */
  override def getOrAddId(category: IdCategory, idLabel: String): Option[Int] =
    ids.get(category).map(_.getOrAddId(idLabel))


  override def get(category: IdCategory, idLabel: String): Option[Int] = {
    ids.get(category).flatMap(_.getId(idLabel))
  }

  /**
   * 通过给定的序列得到对应的对象字符串
   * @param idOrd 对象序列
   * @return 对象字符串
   */
  override def getIdLabel(category:IdCategory,idOrd: Int): Option[String] =
    ids.get(category).flatMap(_.getIdLabel(idOrd))

  class NoSQLCategoryIdService(category:IdCategory){
    private val path = new File(config.id.noSql.path,category.toString)
    FileUtils.forceMkdir(path)
    private val nosqlOptions= new NoSQLOptions()
    nosqlOptions.setCache_size_mb(config.id.noSql.cache)
    nosqlOptions.setWrite_buffer_mb(config.id.noSql.writeBuffer)
    nosqlOptions.setMax_open_files(config.id.noSql.maxOpenFiles)
    private val nosql = new NoSQLSupport(path.getAbsolutePath,nosqlOptions)
    private val globalOrd = new AtomicInteger(loadMaxOrd())
    private def loadMaxOrd():Int ={
      val minOrdKey = buildOrdKey(1)
      val maxOrdKey = buildOrdKey(Integer.MAX_VALUE)

      val bytes = nosql.FindMaxKeyInRange(minOrdKey,maxOrdKey)
      val maxOrd =  if(bytes != null) ByteBuffer.wrap(bytes).getInt(1) else 0
      info("[{}] load max ord :{}",category,maxOrd)

      maxOrd
    }
    private def buildLabelKey(idLabel:String):Array[Byte]={
      val bytes = idLabel.getBytes
      ByteBuffer.allocate(bytes.length+1).put('L'.asInstanceOf[Byte]).put(bytes).array()
    }
    private def buildOrdKey(ord:Int):Array[Byte]={
      ByteBuffer.allocate(5).put('O'.asInstanceOf[Byte]).putInt(ord).array()
    }
    def getOrAddId(idLabel: String): Int = {
      val labelKey = buildLabelKey(idLabel)
      val ordBytes = nosql.Get(labelKey)
      if(ordBytes != null){
        ByteBuffer.wrap(ordBytes).getInt
      }else{
        val ord = globalOrd.incrementAndGet()
        nosql.RawPut(labelKey,DataTypeUtils.convertIntAsArray(ord))
        nosql.RawPut(buildOrdKey(ord),idLabel.getBytes)

        ord
      }
    }
    def getId(idLabel:String):Option[Int]={
      val labelKey = buildLabelKey(idLabel)
      val ordBytes = nosql.Get(labelKey)
      if(ordBytes != null) Some(ByteBuffer.wrap(ordBytes).getInt) else None
    }
    def getIdLabel(idOrd: Int): Option[String] = {
      val labelBytes = nosql.Get(buildOrdKey(idOrd))
      if(labelBytes ==null || labelBytes.isEmpty) None else Some(new String(labelBytes))
    }
    def start():Unit={
    }
    def shutdown(): Unit ={
      ServiceUtils.runInNoThrow{nosql.delete()}
    }
  }
}
