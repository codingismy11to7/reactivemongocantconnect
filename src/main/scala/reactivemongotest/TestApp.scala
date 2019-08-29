package reactivemongotest

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson._

import akka.actor.ActorSystem
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Failure, Properties, Success, Try}

/**
  * @author steven
  *
  */
object TestApp extends App {

  if (args.headOption.contains("run"))
    new Query().run()
  else {
    val jHome = Properties.javaHome
    val exe = "java" + (if (Properties.isWin) ".exe" else "")
    val cp = Properties.javaClassPath
    var exitCode = 0
    while (exitCode == 0) exitCode = s"$jHome/bin/$exe -cp $cp reactivemongotest.TestApp run".!
  }
}

class Query {
  private def block[T](f: => Future[T]) = Try(Await.result(f, Duration.Inf))
  private def sleep(finDur: FiniteDuration): Unit = Thread.sleep(finDur.toMillis)

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

    val log = LoggerFactory.getLogger(getClass)

    val uri = MongoConnection.parseURI("mongodb://localhost:27017").fold(throw _, identity)
    val driver = MongoDriver(config)
    val conn = driver.connection(uri, strictUri = true).fold(throw _, identity)
    def dbF = conn.database("testreactivemongo")

    @tailrec
    def insert(failcount: Int = 0): Unit = {
      val resultF = dbF flatMap { db =>
        val coll = db.collection[BSONCollection]("deleteme")
        val doc = BSONDocument("test" -> BSONString("test"))
        coll.insert(false).one(doc)
      }

      block(resultF) match {
        case Failure(exception) =>
          if (failcount == 25) {
            log.error("hit 25 failures, stopping connection")
            implicit val timeout: FiniteDuration = 1.minute
            log.error("trying to close connection")
            val res = block(conn.askClose())
            log.error("close connection result: {}", res)
            log.error("Pausing for 1 minute - check the mongo.log file for disconnects")
            sleep(1.minute)
            log.error("going to close the driver")
            val res2 = Try(driver.close(1.minute))
            log.error("close driver result: {}", res2)
            log.error("Finishing")
            sys.exit(1)
          } else {
            log.error(" ")
            log.error("Failed! will continue to retry", exception)
            log.error(" ")
            exception.printStackTrace()
            sleep(1.second)
            insert(1 + failcount)
          }

        case Success(value) =>
          println(value)
          implicit val timeout: FiniteDuration = 10.seconds
          block(conn.askClose())
      }
    }

    insert()

    driver.close()
    system.terminate()

    log.info("sucessfully inserted, retrying")
    log.info(" ")
    log.info(" ")

    sleep(500.millis)

    sys.exit(0)
  }
}
