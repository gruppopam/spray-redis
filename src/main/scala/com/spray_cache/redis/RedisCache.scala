package com.spray_cache.redis

import spray.caching.Cache
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.util.Timeout
import com.redis.RedisClient
import com.redis.serialization.Format

case class RedisCache[V](maxCapacity: Int = 500, initialCapacity: Int = 16)
                              (implicit val ec: ExecutionContext,
                               implicit val system: ActorSystem,
                               implicit val timeout: Timeout,
                               implicit val redisClient: RedisClient,
                               implicit val format: Format[V])
  extends Cache[V] {

  private[RedisCache] val store = RedisBackedMap[V](maxCapacity, initialCapacity)

  override def size: Int = store.size

  override def clear(): Unit = store.clear()

  override def remove(key: Any): Option[Future[V]] = Option(store.remove(key.toString))

  override def get(key: Any): Option[Future[V]] = Option(store.get(key.toString))

  override def apply(key: Any, genValue: () => Future[V])(implicit ec: ExecutionContext): Future[V] = {
    store.putIfAbsent(key.asInstanceOf[String], genValue)
  }
}
