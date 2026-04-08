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
package instances

import cats.effect.{Async, Cont, Deferred, Outcome, Ref, Unique}
import cats.effect.kernel.{CancelScope, Poll}
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/** Cats Effect 3 type class instance of [[monix.eval.Task Task]]
  * for `cats.effect.Async` (and implicitly for `Applicative`, `Monad`,
  * `MonadError`, `Sync`, `MonadCancel`, `GenSpawn`, `GenConcurrent`,
  * `GenTemporal`, etc).
  *
  * References:
  *
  *  - [[https://typelevel.org/cats/ typelevel/cats]]
  *  - [[https://github.com/typelevel/cats-effect typelevel/cats-effect]]
  */
class CatsAsyncForTask extends CatsBaseForTask with Async[Task] {

  // --- Clock (from Sync < Async) ---

  override def monotonic: Task[FiniteDuration] =
    Task.deferAction(sc => Task.now(FiniteDuration(sc.clockMonotonic(java.util.concurrent.TimeUnit.NANOSECONDS), java.util.concurrent.TimeUnit.NANOSECONDS)))

  override def realTime: Task[FiniteDuration] =
    Task.deferAction(sc => Task.now(FiniteDuration(sc.clockRealTime(java.util.concurrent.TimeUnit.NANOSECONDS), java.util.concurrent.TimeUnit.NANOSECONDS)))

  // --- Unique ---

  override def unique: Task[Unique.Token] =
    Task.delay(new Unique.Token)

  // --- Sync ---

  override def delay[A](thunk: => A): Task[A] =
    Task.eval(thunk)

  override def defer[A](fa: => Task[A]): Task[A] =
    Task.defer(fa)

  override def blocking[A](thunk: => A): Task[A] =
    Task.eval(thunk)

  override def interruptible[A](thunk: => A): Task[A] =
    Task.eval(thunk)

  override def suspend[A](hint: cats.effect.kernel.Sync.Type)(thunk: => A): Task[A] =
    Task.eval(thunk)

  // --- MonadCancel ---

  override def canceled: Task[Unit] =
    Task.raiseError(new java.util.concurrent.CancellationException("Task was canceled"))

  override def forceR[A, B](fa: Task[A])(fb: Task[B]): Task[B] =
    fa.attempt.flatMap(_ => fb)

  override def onCancel[A](fa: Task[A], fin: Task[Unit]): Task[A] =
    fa.guaranteeCase {
      case monix.execution.ExitCase.Canceled => fin
      case _ => Task.unit
    }

  override def uncancelable[A](body: Poll[Task] => Task[A]): Task[A] = {
    // Task's cancellation model: we wrap the body result in uncancelable.
    // The Poll allows selectively re-enabling cancellation, but in Task's
    // simpler model, we use identity (cancellation points are handled internally).
    body(new Poll[Task] {
      def apply[B](fa: Task[B]): Task[B] = fa
    }).uncancelable
  }

  // --- GenSpawn ---

  override def start[A](fa: Task[A]): Task[cats.effect.Fiber[Task, Throwable, A]] =
    fa.start.map { monixFiber =>
      new cats.effect.Fiber[Task, Throwable, A] {
        def cancel: Task[Unit] = monixFiber.cancel
        def join: Task[Outcome[Task, Throwable, A]] =
          monixFiber.join.attempt.map {
            case Right(a) => Outcome.Succeeded(Task.now(a))
            case Left(_: java.util.concurrent.CancellationException) => Outcome.Canceled()
            case Left(e) => Outcome.Errored(e)
          }
      }
    }

  override def never[A]: Task[A] =
    Task.never

  override def cede: Task[Unit] =
    Task.shift

  override def racePair[A, B](fa: Task[A], fb: Task[B]): Task[Either[
    (Outcome[Task, Throwable, A], cats.effect.Fiber[Task, Throwable, B]),
    (cats.effect.Fiber[Task, Throwable, A], Outcome[Task, Throwable, B])]] =
    Task.racePair(fa, fb).map {
      case Left((a, monixFiberB)) =>
        Left((Outcome.Succeeded(Task.now(a)), wrapFiber(monixFiberB)))
      case Right((monixFiberA, b)) =>
        Right((wrapFiber(monixFiberA), Outcome.Succeeded(Task.now(b))))
    }

  // --- GenConcurrent ---

  override def ref[A](a: A): Task[Ref[Task, A]] = {
    implicit val make: Ref.Make[Task] = Ref.Make.syncInstance(this)
    Ref.of[Task, A](a)
  }

  override def deferred[A]: Task[Deferred[Task, A]] = {
    implicit val async: cats.effect.Async[Task] = this
    Task.delay(Deferred.unsafe[Task, A])
  }

  // --- GenTemporal ---

  override def sleep(time: FiniteDuration): Task[Unit] =
    Task.sleep(time)

  // --- Async ---

  override def evalOn[A](fa: Task[A], ec: ExecutionContext): Task[A] =
    ec match {
      case ref: Scheduler => fa.executeOn(ref, forceAsync = true)
      case _ => fa.executeOn(Scheduler(ec), forceAsync = true)
    }

  override def executionContext: Task[ExecutionContext] =
    Task.deferAction(sc => Task.pure(sc: ExecutionContext))

  override def async[A](k: (Either[Throwable, A] => Unit) => Task[Option[Task[Unit]]]): Task[A] =
    Task.cancelable0 { (_, cb) =>
      // Execute the registration, get optional cancel token
      k(cb).flatMap {
        case Some(cancelToken) => cancelToken.map(_ => ())
        case None => Task.unit
      }
    }

  override def async_[A](k: (Either[Throwable, A] => Unit) => Unit): Task[A] =
    Task.async(k)

  override def cont[K, R](body: Cont[Task, K, R]): Task[R] = {
    // Implementation of cont using Deferred + uncancelable
    // This follows the reference pattern from cats-effect IO
    Task.defer {
      for {
        d <- Deferred[Task, Either[Throwable, K]](this)
        r <- uncancelable { poll =>
          val cb: Either[Throwable, K] => Unit = { result =>
            d.complete(result)
            ()
          }
          val get: Task[K] = poll(d.get).flatMap {
            case Right(k) => Task.now(k)
            case Left(e) => Task.raiseError(e)
          }
          body[Task](this)(cb, get, cats.arrow.FunctionK.id[Task])
        }
      } yield r
    }
  }

  override def fromFuture[A](fut: Task[scala.concurrent.Future[A]]): Task[A] =
    fut.flatMap(f => Task.fromFuture(f))

  private def wrapFiber[A](f: Fiber[A]): cats.effect.Fiber[Task, Throwable, A] =
    new cats.effect.Fiber[Task, Throwable, A] {
      def cancel: Task[Unit] = f.cancel
      def join: Task[Outcome[Task, Throwable, A]] =
        f.join.attempt.map {
          case Right(a) => Outcome.Succeeded(Task.now(a))
          case Left(_: java.util.concurrent.CancellationException) => Outcome.Canceled()
          case Left(e) => Outcome.Errored(e)
        }
    }
}

/** Default and reusable instance for [[CatsAsyncForTask]].
  *
  * Globally available in scope, as it is returned by
  * [[monix.eval.Task.catsAsync Task.catsAsync]].
  */
object CatsAsyncForTask extends CatsAsyncForTask
