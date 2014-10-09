package com.spray_cache.redis

import org.scalatest.{Matchers, GivenWhenThen, FeatureSpec}

import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures._
import redis.RedisClient

class RedisBackedMapSpec extends FeatureSpec with GivenWhenThen with Matchers {

  import spec.SpecHelper._
  import Formats._
  
  info("As a Redis backed Map")
  info("I should be able to perform map like operations for")
  info("1. Put into map")
  info("2. Get from map")
  info("3. Clear the map")

  feature("Construction and defaults") {
    scenario("Initialization and validations") {
      Given("I create a redis backed map with maxCapacity -1")
      implicit val client = MockitoSugar.mock[RedisClient]

      Then("Initialization should fail with an Illegal Argument Exception")
      a[IllegalArgumentException] should be thrownBy {
        RedisBackedMap[String](-1, -1)
      }

      And("No calls to redis should be attempted")
      Mockito verifyNoMoreInteractions client

      Given("I create a redis backed map with initial capacity greater than max capacity")
      Mockito reset client

      Then("Initialization should fail with an Illegal Argument Exception")
      a[IllegalArgumentException] should be thrownBy {
        RedisBackedMap[String](10, 11)
      }

      And("No calls to redis should be attempted")
      Mockito verifyNoMoreInteractions client
    }
  }
  feature("Remove") {
    scenario("should be successful when value exists") {
      Given("I create a valid redis backed map")
      implicit val client = MockitoSugar.mock[RedisClient]
      Mockito.when(client.set("key1", "Hello World")).thenReturn(Future(true))
      val store = RedisBackedMap[String](10, 10)

      And("I add a valid value to the store")
      store.putIfAbsent("key1", () => Future("Hello World"))
      store.size shouldBe 1

      When("I attempt to remove the key 'key1' from the store")
      val result: Future[String] = store.remove("key1")

      Then("The remove operation should be successful")
      whenReady(result) {
        value =>
          new String(value) shouldBe "Hello World"
      }
      Mockito verify client del "key1"
    }

    scenario("should be successful when value does not exists") {
      Given("I create a valid redis backed map")
      implicit val client = MockitoSugar.mock[RedisClient]
      val store = RedisBackedMap[String](10, 10)
      store.size shouldBe 0

      When("I attempt to remove the key 'key1' from the store")
      val result: Future[String] = store.remove("key1")

      Then("The remove operation should be successful")
      result shouldBe null
      Mockito verify client del "key1"
    }
  }

  feature("get") {
    scenario("should return from local store if available") {
      Given("I create a valid redis backed map")
      implicit val client = MockitoSugar.mock[RedisClient]
      Mockito.when(client.set("key1", "Hello World")).thenReturn(Future(true))
      val store = RedisBackedMap[String](10, 10)
      store.size shouldBe 0

      And("I put an entry into the store")
      store.putIfAbsent("key1", () => Future("Hello World"))

      When("I request value for key 'key1'")
      val result: Future[String] = store.get("key1")

      Then("The value should be resolved from the local store")
      whenReady(result) {
        value =>
          new String(value) shouldBe "Hello World"
      }
      Mockito.verify(client, Mockito.never()).get("key1")
    }
  }

}

