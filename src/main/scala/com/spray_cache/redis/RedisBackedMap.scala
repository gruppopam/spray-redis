package com.spray_cache.redis

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.util.{Failure, Success}
import akka.util.Timeout
import scala.concurrent._
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClient}

case class RedisBackedMap[V](maxCapacity: Int, initialCapacity: Int)
                            (implicit val client: RedisClient,
                             implicit val ec: ExecutionContext,
                             implicit val timeout: Timeout) {

  require(maxCapacity > 0, "maxCapacity must be greater than 0")
  require(initialCapacity <= maxCapacity, "initialCapacity must be <= maxCapacity")

  private[RedisBackedMap] val store = new ConcurrentLinkedHashMap.Builder[String, Future[V]]
    .initialCapacity(initialCapacity)
    .maximumWeightedCapacity(maxCapacity)
    .build()

  def putIfAbsent(key: String, genValue: () => Future[V])(implicit serializer: ByteStringSerializer[V],
                                                          deserializer: ByteStringDeserializer[V]): Future[V] = {
    val promise = Promise[V]()
    store.putIfAbsent(key.toString, promise.future) match {
      case null ⇒
        val future = genValue()
        future.onComplete {
          case value@Success(_) ⇒
            val write: Future[Boolean] = redisWrite(key, value get)
            write onComplete {
              case Success(_) => promise.complete(value)
              case Failure(f) =>
                store.remove(key.toString)
                promise.failure(new RuntimeException("Error when persisting to Redis", f))
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

  def get(key: String)(implicit deserializer: ByteStringDeserializer[V]): Future[V] = {
    if (store.containsKey(key)) return store.get(key)
    store.remove(key)

    val fromRedis: Future[V] = redisGet(key)
    fromRedis onComplete {
      case Success(x) => store.put(key, fromRedis)
      case Failure(x) => {
        throw new RuntimeException("Failure when talking to redis", x)
      }
    }
    fromRedis
  }

  def size = store.size()

  def clear() = {
    client.flushall()
    store.clear()
  }

  def redisWrite(key: String, value: V)(implicit serializer: ByteStringSerializer[V]) = {
    client.set(key, value)
  }

  def redisGet(key: String)(implicit deserializer: ByteStringDeserializer[V]) = for {
    res <- client.get[V](key)
  } yield {
    res.get
  }
}