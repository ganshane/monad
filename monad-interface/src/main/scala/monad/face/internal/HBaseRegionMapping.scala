package monad.face.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import org.apache.hadoop.hbase.util.Bytes

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2016-09-09
  */
object HBaseRegionMapping {
  private val regionMapping = new ConcurrentHashMap[String,Int]()
  private val regionIdMapping  = new ConcurrentHashMap[Int,String]()
  private val seq = new AtomicInteger(1)
  def getRegionMappingId(regionKey:String): Int={
    var regionId = 0
    if(regionMapping.containsKey(regionKey)) {
      regionId = regionMapping.get(regionKey)
    }else{
      regionId = seq.incrementAndGet()
      val r = regionMapping.put(regionKey,regionId)
      if(r == 0)
        regionIdMapping.put(regionId,regionKey)
    }

    regionId
  }
  def findRegionKeyById(regionId:Int):Array[Byte]={
    val regionKey = regionIdMapping.get(regionId)
    if(regionKey != null)
      Bytes.toBytes(regionKey)
    else
      throw new RuntimeException("region key not found by id:"+regionId)
  }
}
