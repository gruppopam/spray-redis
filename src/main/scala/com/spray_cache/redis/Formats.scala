package com.spray_cache.redis

import redis.{ByteStringSerializer, ByteStringFormatter}
import ByteStringSerializer._
import akka.util.ByteString

object Formats {
  implicit val format = new ByteStringFormatter[Array[Byte]] {
    override def deserialize(bs: ByteString): Array[Byte] = bs.toArray[Byte]
    override def serialize(data: Array[Byte]): ByteString = ArrayByteConverter.serialize(data)
  }

}
