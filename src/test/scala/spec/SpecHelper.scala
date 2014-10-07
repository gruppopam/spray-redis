package spec

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import com.redis.RedisClient


object SpecHelper {
  implicit val system = ActorSystem("spray-redis-cache")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 second)

  def clearRedis(implicit client: RedisClient) = {
    client.flushall()
  }
}