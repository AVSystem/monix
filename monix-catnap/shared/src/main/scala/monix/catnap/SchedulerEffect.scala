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

import cats.effect._
import monix.execution.Scheduler
import monix.execution.internal.AttemptCallback.RunnableTick

import scala.concurrent.duration.FiniteDuration

object SchedulerEffect {

  /** Schedules a sleep using the given [[monix.execution.Scheduler Scheduler]],
    * returning a cancelable effect.
    *
    * In Cats Effect 3, `Timer` and `ContextShift` no longer exist as separate
    * type classes — `sleep` is on `Temporal` and `evalOn`/`cede` are on `Async`.
    * This utility is provided for cases where you need to schedule a sleep
    * on a specific Scheduler.
    */
  def sleep[F[_]](source: Scheduler, duration: FiniteDuration)(implicit F: Async[F]): F[Unit] =
    F.async { cb =>
      val token = source.scheduleOnce(duration.length, duration.unit, new RunnableTick(cb))
      F.pure(Some(F.delay(token.cancel())))
    }

  /** Returns the real time from the given [[monix.execution.Scheduler Scheduler]].
    */
  def realTime[F[_]](source: Scheduler)(implicit F: Sync[F]): F[FiniteDuration] =
    F.delay(FiniteDuration(source.clockRealTime(java.util.concurrent.TimeUnit.NANOSECONDS), java.util.concurrent.TimeUnit.NANOSECONDS))

  /** Returns the monotonic time from the given [[monix.execution.Scheduler Scheduler]].
    */
  def monotonic[F[_]](source: Scheduler)(implicit F: Sync[F]): F[FiniteDuration] =
    F.delay(FiniteDuration(source.clockMonotonic(java.util.concurrent.TimeUnit.NANOSECONDS), java.util.concurrent.TimeUnit.NANOSECONDS))
}
