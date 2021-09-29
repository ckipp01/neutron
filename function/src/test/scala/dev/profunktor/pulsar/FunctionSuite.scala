/*
 * Copyright 2020 Chatroulette
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

package dev.profunktor.pulsar

import dev.profunktor.pulsar.FunctionInput._

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object FunctionSuite extends SimpleIOSuite with Checkers {
  test("Function can convert numbers to strings") {
    forall { n: Int =>
      val f = new Function[Int, String] {
        override def handle(input: Int, ctx: Context): String =
          input.toString
      }

      val result = f.process(n, emptyCtx)
      expect.same(result, n.toString)
    }
  }

  test("Function can do side effects") {
    forall { n: Int =>
      var i = 0
      val f = new Function[Int, Unit] {
        override def handle(input: Int, ctx: Context): Unit =
          i = input
      }

      f.process(n, emptyCtx)
      expect.same(i, n)
    }
  }
}
