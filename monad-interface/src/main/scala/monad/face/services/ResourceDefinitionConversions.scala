package monad.face.services

import java.sql.ResultSet

import com.google.gson.JsonObject
import monad.face.model.ResourceDefinition.ResourceProperty
import monad.face.model.types._
import monad.face.model.{ColumnType, IndexType, MonadColumnType}
import monad.support.services.MonadException
import org.apache.lucene.document.Field

import scala.util.control.NonFatal

/**
 * ResourceDefinition Conversions
 * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
 * @since 2015-03-01
 */
trait ResourceDefinitionConversions {
  implicit def indexTypeWrapper(it: IndexType) = new {
    def indexType() = it match {
      case IndexType.Text =>
        Field.Index.ANALYZED
      case IndexType.Keyword =>
        Field.Index.NOT_ANALYZED
      case IndexType.UnIndexed =>
        Field.Index.NO
    }
  }

  implicit def wrapColumnType(ct: ColumnType) = new {
    def getColumnType: MonadColumnType[_] = ct match {
      case ColumnType.String =>
        StringColumnType
      case ColumnType.Clob =>
        ClobColumnType
      case ColumnType.Date =>
        DateColumnType
      case ColumnType.Int =>
        IntColumnType
      case ColumnType.Long =>
        LongColumnType
    }
  }
  implicit def resourcePropertyOps(rp: ResourceProperty) = new {
    def createIndexField(value: Any): Field = {
      rp.columnType.getColumnType.asInstanceOf[MonadColumnType[Any]].createIndexField(value, rp)
    }

    def setIndexValue(f: Field, value: Any) {
      rp.columnType.getColumnType.asInstanceOf[MonadColumnType[Any]].setIndexValue(f, value, rp)
    }

    def isToken: Boolean = {
      rp.indexType.indexType() == Field.Index.ANALYZED
    }

    def isKeyword: Boolean = {
      rp.indexType.indexType() == Field.Index.NOT_ANALYZED
    }

    def isNumeric: Boolean = {
      rp.columnType == ColumnType.Long || rp.columnType == ColumnType.Int || rp.columnType == ColumnType.Date
    }

    def readJdbcValue(rs: ResultSet, index: Int) = {
      rp.columnType.getColumnType.readValueFromJdbc(rs, index, rp)
    }

    def readDfsValue(dbObj: JsonObject) = {
      try {
        rp.columnType.getColumnType.readValueFromDfs(dbObj, rp)
      } catch {
        case NonFatal(e) =>
          throw new MonadException("unable to read value from dfs with name:" + rp.name, e, null)
      }
    }

    def readApiValue(dbObj: JsonObject) = {
      rp.columnType.getColumnType.asInstanceOf[MonadColumnType[Any]].convertDfsValueToString(readDfsValue(dbObj), rp)
    }
  }
}

object ResourceDefinitionConversions extends ResourceDefinitionConversions {}
