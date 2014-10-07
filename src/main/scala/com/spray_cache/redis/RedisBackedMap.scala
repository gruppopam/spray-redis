package com.spray_cache.redis

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.util.{Failure, Success}
import com.redis.RedisClient
import akka.util.Timeout
import akka.actor.ActorSystem
import scala.concurrent._
import com.redis.serialization.{DefaultFormats, Format}
import DefaultFormats._

case class RedisBackedMap[V](maxCapacity: Int, initialCapacity: Int)
                            (implicit val client: RedisClient,
                             implicit val timeout: Timeout,
                             implicit val system: ActorSystem,
                             implicit val executionContext: ExecutionContext) {

  require(maxCapacity > 0, "maxCapacity must be greater than 0")
  require(initialCapacity <= maxCapacity, "initialCapacity must be <= maxCapacity")

  private[RedisBackedMap] val store = new ConcurrentLinkedHashMap.Builder[String, Future[V]]
    .initialCapacity(initialCapacity)
    .maximumWeightedCapacity(maxCapacity)
    .build()

  def putIfAbsent(key: String, genValue: () => Future[V])(implicit format: Format[V]): Future[V] = {
    val promise = Promise[V]()
    store.putIfAbsent(key.toString, promise.future) match {
      case null ⇒
        val future = genValue()
        future.onComplete {
          case value@Success(_) ⇒
            val write: Future[Boolean] = redisWrite(key, value get)
            write onComplete {
              case Success(_) => promise.complete(value)
              case Failure(_) => promise.failure(new RuntimeException("Error when persisting to Redis"))
            }
          case value@Failure(_) ⇒
            store.remove(key.toString)
            promise.complete(value)
        }
        promise.future
      case existingFuture ⇒ existingFuture
    }

  }

  def remove(key: String): Future[V] = {
    client.del(key)
    store.remove(key)
  }

  def get(key: String)(implicit format: Format[V]): Future[V] = {
    if (store.containsKey(key)) return store.get(key)
    store.remove(key)

    val fromRedis: Future[V] = redisGet(key)
    fromRedis onComplete {
      case Success(x) => store.put(key, fromRedis)
      case Failure(x) => {
        println(x)
        throw new RuntimeException("Failure when talking to redis")
      }
    }
    fromRedis
  }

  def size = store.size()

  def clear() = {
    client.flushall()
    store.clear()
  }

  def redisWrite(key: String, value: V)(implicit format: Format[V]) = {
    client.set(key, value)
  }

  def redisGet(key: String)(implicit format: Format[V]) = for {
    res <- client.get[V](key)
  } yield {
    res.get
  }
}