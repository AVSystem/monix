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

package monix.eval

import cats.effect.{Async, Temporal}
import monix.execution.exceptions.DummyException
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Tests for Task's CE3 temporal/async/clock functionality.
  *
  * In CE3, Timer, ContextShift and Clock are no longer separate type classes.
  * Clock is built into Sync, Timer into Temporal, ContextShift into Async.
  */
object TaskClockTimerAndContextShiftSuite extends BaseTestSuite {
  test("Task monotonic clock via Temporal") { implicit s =>
    s.tick(1.seconds)

    val f = Temporal[Task].monotonic.runToFuture
    s.tick()

    assertEquals(f.value, Some(Success(1.second)))
  }

  test("Task realTime clock via Temporal") { implicit s =>
    s.tick(1.seconds)

    val f = Temporal[Task].realTime.runToFuture
    s.tick()

    assertEquals(f.value, Some(Success(1.second)))
  }

  test("Task sleep via Temporal") { implicit s =>
    val f = Temporal[Task].sleep(1.second).runToFuture
    s.tick()
    assertEquals(f.value, None)
    s.tick(1.second)
    assertEquals(f.value, Some(Success(())))
  }

  test("Task cede via Async") { implicit s =>
    val f = Async[Task].cede.runToFuture
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Success(())))
  }

  test("Task evalOn via Async") { implicit s =>
    val s2 = TestScheduler()
    val f = Async[Task].evalOn(Task(1), s2).runToFuture

    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, None)
    s2.tick()
    s.tick()
    assertEquals(f.value, Some(Success(1)))
  }

  test("Task evalOn via Async - failure") { implicit s =>
    val s2 = TestScheduler()
    val dummy = DummyException("dummy")
    val f = Async[Task].evalOn(Task.raiseError(dummy), s2).runToFuture

    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, None)
    s2.tick()
    assertEquals(f.value, None)
    s.tick()
    assertEquals(f.value, Some(Failure(dummy)))
  }
}
