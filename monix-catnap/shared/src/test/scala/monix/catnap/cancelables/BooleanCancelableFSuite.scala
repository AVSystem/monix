/*
 * Copyright (c) 2014-2021 by The Monix Project Developers.
 * See the project homepage at: https://monix.io
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

package monix.catnap
package cancelables

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import minitest.SimpleTestSuite

object BooleanCancelableFSuite extends SimpleTestSuite {

  private def unsafeRun[A](io: IO[A]): A =
    io.unsafeToFuture().value.get.get

  test("apply") {
    var effect = 0
    val task = IO { effect += 1 }
    val ref = BooleanCancelableF[IO](task)

    val cf = unsafeRun(ref)
    assert(!unsafeRun(cf.isCanceled), "!cf.isCanceled")
    assertEquals(effect, 0)
    unsafeRun(cf.cancel)
    assert(unsafeRun(cf.isCanceled), "cf.isCanceled")
    assertEquals(effect, 1)
    unsafeRun(cf.cancel)
    assert(unsafeRun(cf.isCanceled), "cf.isCanceled")
    assertEquals(effect, 1)

    // Referential transparency test
    val cf2 = unsafeRun(ref)
    assert(!unsafeRun(cf2.isCanceled), "!cf2.isCanceled")
    assertEquals(effect, 1)
    unsafeRun(cf2.cancel)
    assert(unsafeRun(cf2.isCanceled), "cf2.isCanceled")
    assertEquals(effect, 2)
    unsafeRun(cf2.cancel)
    assert(unsafeRun(cf2.isCanceled), "cf2.isCanceled")
    assertEquals(effect, 2)
  }

  test("alreadyCanceled") {
    val cf = BooleanCancelableF.alreadyCanceled[IO]
    assert(unsafeRun(cf.isCanceled), "cf.isCanceled")
    unsafeRun(cf.cancel)
    unsafeRun(cf.cancel)
    assert(unsafeRun(cf.isCanceled), "cf.isCanceled")
  }

  test("dummy") {
    val cf = BooleanCancelableF.dummy[IO]
    assert(!unsafeRun(cf.isCanceled), "!cf.isCanceled")
    unsafeRun(cf.cancel)
    unsafeRun(cf.cancel)
    assert(!unsafeRun(cf.isCanceled), "!cf.isCanceled")
  }
}
