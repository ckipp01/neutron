/*
 * Copyright 2021 ProfunKtor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.profunktor.pulsar.schema.circe

import java.nio.charset.StandardCharsets.UTF_8

import cats.Inject
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._

private[circe] trait JsonAsBytes {
  implicit def circeInjectAsBytes[T: Decoder: Encoder]: Inject[T, Array[Byte]] =
    new Inject[T, Array[Byte]] {
      val inj: T => Array[Byte] =
        _.asJson.noSpaces.getBytes(UTF_8)

      val prj: Array[Byte] => Option[T] =
        bytes => decode[T](new String(bytes, UTF_8)).toOption
    }
}
