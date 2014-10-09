package com.spray_cache.redis

import org.scalatest.{Matchers, GivenWhenThen, FeatureSpec}

import scala.concurrent.{Await, Future}
import org.scalatest.concurrent.ScalaFutures._
import scala.concurrent.duration._
import redis.RedisClient



class RedisBackedMapIntegrationSpec extends FeatureSpec with GivenWhenThen with Matchers {

  import spec.SpecHelper._
  import Formats._


  implicit val client = RedisClient()

  info("Redis Backed Map with the real redis")

  feature("get") {
    scenario("should read from redis if not present in the local store") {
      Given("I create a valid empty redis backed map")
      clearRedis
      val store = RedisBackedMap[Array[Byte]](10, 10)
      store.size shouldBe 0

      And("I add a valid entry to the redis store")
      val write: Future[Boolean] = store.redisWrite("key1", "Hello World" getBytes)
      Await.result(write, 5 seconds)

      When("I request value for key 'key1'")
      val result: Future[Array[Byte]] = store.get("key1")

      Then("The value should be read from redis")
      whenReady(result) {
        case (x: Array[Byte]) => new String(x) shouldBe "Hello World"
        case (_) => fail("Value not found")
      }
      store.size shouldBe 1

    }
  }
  feature("putIfAbsent") {

    scenario("should insert if key is not found in the store") {

      Given("I create a valid empty redis backed map")
      clearRedis
      val store = RedisBackedMap[String](10, 10)
      store.size shouldBe 0

      val response: Future[Any] = client.get[String]("key")
      whenReady(response) {
        case (None) => None
        case (x: Any) => {
          fail("Please perform proper clean up before running the test")
        }
      }

      When("I attempt to add a value 'Hello World' with key 'key'")
      val result: Future[String] = store.putIfAbsent("key", () => Future("Hello World"))

      Then("The store should reflect the insertion")
      whenReady(result) {
        value =>
          store.size shouldBe 1
      }

      And("The k,v should also be present in redis")
      whenReady(store.redisGet("key")) {
        case (x: String) => x shouldBe "Hello World"
        case (_) => fail("Cant read from redis")
      }

      And("I clean my own backyard")
      store.clear()
      store.size shouldBe 0
      system.shutdown()
    }
  }
}
