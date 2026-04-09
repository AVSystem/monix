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

package monix.eval.internal

import cats.effect._
import monix.eval.Task
import monix.execution.rstreams.SingleAssignSubscription
import org.reactivestreams.{Publisher, Subscriber}

private[eval] object TaskConversions {
  /**
    * Implementation for `Task#toIO`.
    */
  def toIO[A](source: Task[A])(implicit async: Async[Task]): IO[A] =
    source match {
      case Task.Now(value) => IO.pure(value)
      case Task.Error(e) => IO.raiseError(e)
      case Task.Eval(thunk) => IO(thunk())
      case _ =>
        IO.async[A] { cb =>
          // Run the task and feed results into the IO callback
          implicit val s = monix.execution.Scheduler.global
          val cancelable = source.runAsync {
            case Right(a) => cb(Right(a))
            case Left(e) => cb(Left(e))
          }
          IO.pure(Some(IO.delay(cancelable.cancel())))
        }
    }

  /**
    * Implementation for `Task.fromReactivePublisher`.
    */
  def fromReactivePublisher[A](source: Publisher[A]): Task[Option[A]] =
    Task.cancelable0 { (scheduler, cb) =>
      val sub = SingleAssignSubscription()

      source.subscribe(new Subscriber[A] {
        private[this] var isActive = true

        def onSubscribe(s: org.reactivestreams.Subscription): Unit = {
          sub := s
          sub.request(1)
        }

        def onNext(a: A): Unit = {
          if (isActive) {
            isActive = false
            sub.cancel()
            cb.onSuccess(Some(a))
          }
        }

        def onError(e: Throwable): Unit = {
          if (isActive) {
            isActive = false
            cb.onError(e)
          } else {
            scheduler.reportFailure(e)
          }
        }

        def onComplete(): Unit = {
          if (isActive) {
            isActive = false
            cb.onSuccess(None)
          }
        }
      })

      Task(sub.cancel())
    }
}
