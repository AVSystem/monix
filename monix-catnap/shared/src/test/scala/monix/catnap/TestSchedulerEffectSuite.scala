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

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import minitest.TestSuite
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration._
import scala.util.Success

object TestSchedulerEffectSuite extends TestSuite[TestScheduler] {
  def setup() = TestScheduler()
  def tearDown(env: TestScheduler): Unit = {
    assert(env.state.tasks.isEmpty)
  }

  test("monotonic") { s =>
    val fetch = SchedulerEffect.monotonic[IO](s)

    assertEquals(fetch.unsafeRunSync(), 0.seconds)
    s.tick(5.seconds)
    assertEquals(fetch.unsafeRunSync(), 5.seconds)
    s.tick(5.seconds)
    assertEquals(fetch.unsafeRunSync(), 10.seconds)
    s.tick(300.millis)
    assertEquals(fetch.unsafeRunSync(), 10300.millis)
  }

  test("realTime") { s =>
    val fetch = SchedulerEffect.realTime[IO](s)

    assertEquals(fetch.unsafeRunSync(), 0.seconds)
    s.tick(5.seconds)
    assertEquals(fetch.unsafeRunSync(), 5.seconds)
    s.tick(5.seconds)
    assertEquals(fetch.unsafeRunSync(), 10.seconds)
    s.tick(300.millis)
    assertEquals(fetch.unsafeRunSync(), 10300.millis)
  }

  test("sleep") { s =>
    val f = SchedulerEffect.sleep[IO](s, 10.seconds).unsafeToFuture()
    assertEquals(f.value, None)

    s.tick(5.seconds)
    assertEquals(f.value, None)

    s.tick(5.seconds)
    assertEquals(f.value, Some(Success(())))
  }
}
