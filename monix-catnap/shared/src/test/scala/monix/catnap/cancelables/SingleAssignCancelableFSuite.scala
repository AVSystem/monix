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
import monix.execution.exceptions.{CompositeException, DummyException}

import scala.concurrent.Await
import scala.concurrent.duration._

object SingleAssignCancelableFSuite extends SimpleTestSuite {
  test("cancel") {
    var effect = 0
    val s = Await.result(SingleAssignCancelableF[IO].unsafeToFuture(), 5.seconds)
    val b = BooleanCancelableF.unsafeApply(IO { effect += 1 })

    Await.result(s.set(b).unsafeToFuture(), 5.seconds)
    assert(!Await.result(s.isCanceled.unsafeToFuture(), 5.seconds), "!s.isCanceled")

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(Await.result(s.isCanceled.unsafeToFuture(), 5.seconds), "s.isCanceled")
    assert(Await.result(b.isCanceled.unsafeToFuture(), 5.seconds))
    assert(effect == 1)

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(effect == 1)
  }

  test("cancel (plus one)") {
    var effect = 0
    val extra = BooleanCancelableF.unsafeApply(IO { effect += 1 })
    val b = BooleanCancelableF.unsafeApply(IO { effect += 2 })

    val s = Await.result(SingleAssignCancelableF.plusOne(extra).unsafeToFuture(), 5.seconds)
    Await.result(s.set(b).unsafeToFuture(), 5.seconds)

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(Await.result(s.isCanceled.unsafeToFuture(), 5.seconds))
    assert(Await.result(b.isCanceled.unsafeToFuture(), 5.seconds))
    assert(Await.result(extra.isCanceled.unsafeToFuture(), 5.seconds))
    assert(effect == 3)

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(effect == 3)
  }

  test("cancel on single assignment") {
    val s = Await.result(SingleAssignCancelableF[IO].unsafeToFuture(), 5.seconds)
    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(Await.result(s.isCanceled.unsafeToFuture(), 5.seconds))

    var effect = 0
    val b = BooleanCancelableF.unsafeApply(IO { effect += 1 })
    Await.result(s.set(b).unsafeToFuture(), 5.seconds)

    assert(Await.result(b.isCanceled.unsafeToFuture(), 5.seconds))
    assert(effect == 1)

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(effect == 1)
  }

  test("cancel on single assignment (plus one)") {
    var effect = 0
    val extra = BooleanCancelableF.unsafeApply(IO { effect += 1 })
    val s = Await.result(SingleAssignCancelableF.plusOne(extra).unsafeToFuture(), 5.seconds)

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(Await.result(s.isCanceled.unsafeToFuture(), 5.seconds), "s.isCanceled")
    assert(Await.result(extra.isCanceled.unsafeToFuture(), 5.seconds), "extra.isCanceled")
    assert(effect == 1)

    val b = BooleanCancelableF.unsafeApply(IO { effect += 1 })
    Await.result(s.set(b).unsafeToFuture(), 5.seconds)

    assert(Await.result(b.isCanceled.unsafeToFuture(), 5.seconds))
    assert(effect == 2)

    Await.result(s.cancel.unsafeToFuture(), 5.seconds)
    assert(effect == 2)
  }

  test("throw exception on multi assignment") {
    val s = Await.result(SingleAssignCancelableF[IO].unsafeToFuture(), 5.seconds)
    val b1 = CancelableF.empty[IO]
    Await.result(s.set(b1).unsafeToFuture(), 5.seconds)

    val f = s.set(CancelableF.empty[IO]).unsafeToFuture()
    Await.ready(f, 5.seconds)
    assert(f.value.get.isFailure && f.value.get.failed.get.isInstanceOf[IllegalStateException])
    ()
  }

  test("throw exception on multi assignment when canceled") {
    val s = Await.result(SingleAssignCancelableF[IO].unsafeToFuture(), 5.seconds)
    Await.result(s.cancel.unsafeToFuture(), 5.seconds)

    val b1 = CancelableF.empty[IO]
    Await.result(s.set(b1).unsafeToFuture(), 5.seconds)

    val f = s.set(CancelableF.empty[IO]).unsafeToFuture()
    Await.ready(f, 5.seconds)
    assert(f.value.get.isFailure && f.value.get.failed.get.isInstanceOf[IllegalStateException])
    ()
  }

  test("cancel when both reference and `extra` throw") {
    var effect = 0
    val dummy1 = DummyException("dummy1")

    val extra = CancelableF.unsafeApply[IO](IO { effect += 1; throw dummy1 })
    val s = Await.result(SingleAssignCancelableF.plusOne(extra).unsafeToFuture(), 5.seconds)

    val dummy2 = DummyException("dummy2")
    val b = CancelableF.unsafeApply[IO](IO { effect += 1; throw dummy2 })
    Await.result(s.set(b).unsafeToFuture(), 5.seconds)

    try {
      Await.result(s.cancel.unsafeToFuture(), 5.seconds)
      fail("should have thrown")
    } catch {
      case CompositeException((_: DummyException) :: (_: DummyException) :: Nil) =>
        ()
      case other: Throwable =>
        throw other
    }
    assertEquals(effect, 2)
  }
}
