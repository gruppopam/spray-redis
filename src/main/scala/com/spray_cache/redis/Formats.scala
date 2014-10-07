package com.spray_cache.redis

import com.redis.serialization.Format

object Formats {
  implicit val byteArrayFormat = Format[Array[Byte]](x => x.getBytes, x => new String(x))
}
