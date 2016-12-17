package textboard.utils

import com.github.tototoshi.slick.PostgresJodaSupport._
import main.scala.textboard._
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._
import slick.ast.TypedType

trait DateTimeHelper {
  implicit val now = DateTime.now
}
  
trait CustomColumnTypes {
  implicit val jodaDateTimeType =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime, UTC))
}