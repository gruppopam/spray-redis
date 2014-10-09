package com.spray_cache

import _root_.redis.RedisClient
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._


package object cache {

   object RedisSystem {
     implicit val system = ActorSystem("spray-redis-cache")
     implicit val executionContext = system.dispatcher
     implicit val timeout = Timeout(5 second)
     implicit val client = RedisClient()
   }
 }
