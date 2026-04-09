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
import minitest.SimpleTestSuite
import monix.execution.schedulers.ReferenceSchedulerSuite.DummyScheduler

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success

class ReferenceSchedulerEffectSuite extends SimpleTestSuite {
  test("monotonic") {
    val s = new DummyScheduler
    val clockMonotonic = Await.result(SchedulerEffect.monotonic[IO](s).unsafeToFuture(), 5.seconds)
    assert(clockMonotonic > 0.seconds)
  }

  test("realTime") {
    val s = new DummyScheduler
    val clockRealTime = Await.result(SchedulerEffect.realTime[IO](s).unsafeToFuture(), 5.seconds)
    assert(clockRealTime > 0.seconds)
  }

  test("sleep") {
    val s = new DummyScheduler

    val f = SchedulerEffect.sleep[IO](s, 10.seconds).unsafeToFuture()
    assertEquals(f.value, None)

    s.tick(5.seconds)
    assertEquals(f.value, None)

    s.tick(5.seconds)
    assertEquals(f.value, Some(Success(())))
  }
}
