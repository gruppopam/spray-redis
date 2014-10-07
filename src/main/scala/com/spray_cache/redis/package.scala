package com.spray_cache

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import com.redis.RedisClient

package object cache {

   object RedisSystem {
     implicit val system = ActorSystem("spray-redis-cache")
     implicit val executionContext = system.dispatcher
     implicit val timeout = Timeout(5 second)
     implicit val client = RedisClient("localhost", 6379)
   }
 }
