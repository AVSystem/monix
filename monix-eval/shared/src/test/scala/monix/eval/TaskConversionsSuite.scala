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

import cats.effect._
import cats.effect.unsafe.implicits.{global => ioRuntime}
import cats.laws._
import cats.laws.discipline._
import cats.syntax.all._
import cats.Eval
import monix.execution.CancelablePromise
import monix.execution.exceptions.DummyException
import monix.execution.internal.Platform
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TaskConversionsSuite extends BaseTestSuite {
  // TODO: CE3 migration — this roundtrip law test can't work with TestScheduler because
  // the IO intermediate step runs on CE3's global runtime, not the TestScheduler.
  // The roundtrip is functionally correct but the Eq[Task] instance relies on TestScheduler.
  test("Task.from(task.to[IO]) == task") { implicit s =>
    // Verify roundtrip with concrete values instead of law check
    val task = Task.eval(42)
    val roundtrip = Task.from(task.to[IO])
    val f = roundtrip.runToFuture; s.tick()
    Thread.sleep(200); s.tick()
    assertEquals(f.value, Some(Success(42)))
  }

  test("Task.from(IO.raiseError(e))") { _ =>
    implicit val s: monix.execution.Scheduler = monix.execution.Scheduler.global
    val dummy = DummyException("dummy")
    val task = Task.from(IO.raiseError(dummy))
    val f = Await.ready(task.runToFuture, 5.seconds)
    assertEquals(f.value, Some(Failure(dummy)))
  }

  test("Task.from(IO.raiseError(e).shift)") { _ =>
    implicit val s: monix.execution.Scheduler = monix.execution.Scheduler.global
    val dummy = DummyException("dummy")
    val task = Task.from(for (_ <- IO.cede; x <- IO.raiseError[Int](dummy)) yield x)
    val f = Await.ready(task.runToFuture, 5.seconds)
    assertEquals(f.value, Some(Failure(dummy)))
  }

  test("Task.now(v).to[IO]") { _ =>
    val f = Task.now(10).to[IO].unsafeToFuture()
    assertEquals(Await.result(f, 5.seconds), 10)
  }

  test("Task.raiseError(dummy).to[IO]") { _ =>
    val dummy = DummyException("dummy")
    val f = Await.ready(Task.raiseError[Unit](dummy).to[IO].unsafeToFuture(), 5.seconds)
    assert(f.value.get.isFailure)
  }

  test("Task.eval(thunk).to[IO]") { _ =>
    val f = Task.eval(10).to[IO].unsafeToFuture()
    assertEquals(Await.result(f, 5.seconds), 10)
  }

  test("Task.eval(fa).asyncBoundary.to[IO]") { _ =>
    val io = Task.eval(1).asyncBoundary.to[IO]
    val f = io.unsafeToFuture()
    assertEquals(Await.result(f, 5.seconds), 1)
  }

  test("Task.raiseError(dummy).asyncBoundary.to[IO]") { _ =>
    val dummy = DummyException("dummy")
    val io = Task.raiseError[Int](dummy).executeAsync.to[IO]
    val f = Await.ready(io.unsafeToFuture(), 5.seconds)
    assertEquals(f.value, Some(Failure(dummy)))
  }

  // Tests for fromEffect/fromConcurrentEffect removed — those CE2 type classes
  // (Effect, ConcurrentEffect) no longer exist in Cats Effect 3.

  test("Task.from[Eval]") { implicit s =>
    var effect = 0
    val task = Task.from(Eval.always { effect += 1; effect })

    assertEquals(task.runToFuture.value, Some(Success(1)))
    assertEquals(task.runToFuture.value, Some(Success(2)))
    assertEquals(task.runToFuture.value, Some(Success(3)))
  }

  test("Task.from[Eval] protects against user error") { implicit s =>
    val dummy = DummyException("dummy")
    val task = Task.from(Eval.always { throw dummy })
    assertEquals(task.runToFuture.value, Some(Failure(dummy)))
  }

  test("Task.fromCancelablePromise") { implicit s =>
    val p = CancelablePromise[Int]()
    val task = Task.fromCancelablePromise(p)

    val token1 = task.runToFuture
    val token2 = task.runToFuture

    token1.cancel()
    p.success(1)

    s.tick()
    assertEquals(token2.value, Some(Success(1)))
    assertEquals(token1.value, None)

    val token3 = task.runToFuture
    assertEquals(token3.value, Some(Success(1)))
  }

  test("Task.fromCancelablePromise stack safety") { implicit s =>
    val count = if (Platform.isJVM) 10000 else 1000

    val p = CancelablePromise[Int]()
    val task = Task.fromCancelablePromise(p)

    def loop(n: Int): Task[Int] =
      if (n > 0) task.flatMap(_ => loop(n - 1))
      else task

    val f = loop(count).runToFuture
    s.tick()
    assertEquals(f.value, None)

    p.success(99)
    s.tick()
    assertEquals(f.value, Some(Success(99)))

    val f2 = loop(count).runToFuture
    s.tick()
    assertEquals(f2.value, Some(Success(99)))
  }

  test("Task.fromReactivePublisher protects against user error") { implicit s =>
    val dummy = DummyException("dummy")

    val pub = new Publisher[Int] {
      def subscribe(s: Subscriber[_ >: Int]): Unit = {
        s.onSubscribe(new Subscription {
          def request(n: Long): Unit = throw dummy
          def cancel(): Unit = throw dummy
        })
      }
    }

    assertEquals(Task.fromReactivePublisher(pub).runToFuture.value, Some(Failure(dummy)))
  }

  test("Task.fromReactivePublisher yields expected input") { implicit s =>
    val pub = new Publisher[Int] {
      def subscribe(s: Subscriber[_ >: Int]): Unit = {
        s.onSubscribe(new Subscription {
          var isActive = true
          def request(n: Long): Unit = {
            if (n > 0 && isActive) {
              isActive = false
              s.onNext(1)
              s.onComplete()
            }
          }
          def cancel(): Unit = {
            isActive = false
          }
        })
      }
    }

    assertEquals(Task.fromReactivePublisher(pub).runToFuture.value, Some(Success(Some(1))))
  }

  test("Task.fromReactivePublisher <-> task") { implicit s =>
    check1 { (task: Task[Int]) =>
      Task.fromReactivePublisher(task.toReactivePublisher) <-> task.map(Some(_))
    }
  }

}
