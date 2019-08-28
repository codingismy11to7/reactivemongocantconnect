package reactivemongotest

import ch.qos.logback.classic.{Level, LoggerContext}
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson._

import akka.actor.ActorSystem
import sys.process._
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Properties

/**
  * @author steven
  *
  */
object TestApp extends App {
  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO)
//  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger("reactivemongo").setLevel(Level.DEBUG)

  if (args.headOption.contains("run"))
    Query.run()
  else {
    val jHome = Properties.javaHome
    val exe = "java" + (if (Properties.isWin) ".exe" else "")
    val cp = Properties.javaClassPath
    while (true) s"$jHome/bin/$exe -cp $cp reactivemongotest.TestApp run".!
  }
}

object Query {
  private val config = ConfigFactory.parseString(
    """akka {
      |  loggers = ["akka.event.slf4j.Slf4jLogger"]
      |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
      |  loglevel = DEBUG
      |}
      |""".stripMargin
  ) withFallback ConfigFactory.load()

  def run(): Unit = {
    val system = ActorSystem("test", config)
    import system.dispatcher
    val uri = MongoConnection.parseURI("mongodb://localhost:27017").fold(throw _, identity)
    val driver = MongoDriver(config)
    val conn = driver.connection(uri, strictUri = true).fold(throw _, identity)
    def dbF = conn.database("testreactivemongo")
    @tailrec
    def insert(): Unit = {
      val resultF = dbF flatMap { db =>
        val coll = db.collection[BSONCollection]("deleteme")
        val doc = BSONDocument("test" -> BSONString("test"))
        coll.insert(false).one(doc)
      }
      Await.ready(resultF, Duration.Inf)
      val value = resultF.value.get
      if (value.isFailure) {
        println("\nFailed! will continue to retry\n")
        value.failed.get.printStackTrace()
        Thread.sleep(1000)
        insert()
      } else {
        println(value)
        implicit val timeout: FiniteDuration = 10.seconds
        Await.ready(conn.askClose(), Duration.Inf)
      }
    }

    insert()

    driver.close()
    system.terminate()

    println("sucessfully inserted, retrying\n\n")

    Thread.sleep(500)
  }
}
