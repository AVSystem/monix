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

import java.util.concurrent.CompletableFuture
import cats.effect.{Async, IO}
import cats.effect.unsafe.implicits.global
import minitest.TestSuite
import monix.catnap.syntax._
import monix.execution.exceptions.DummyException
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object FutureLiftJava8Suite extends TestSuite[Unit] {
  def setup() = ()
  def tearDown(env: Unit): Unit = ()

  test("convert from async CompletableFuture; on success; with Async[IO]") { _ =>
    val future = new CompletableFuture[Int]()
    val f = IO(future).futureLift.unsafeToFuture()

    Thread.sleep(100)
    assertEquals(f.value, None)

    future.complete(100)
    assertEquals(Await.result(f, 5.seconds), 100)
  }

  test("convert from async CompletableFuture; on success; with Concurrent[IO]") { _ =>
    val future = new CompletableFuture[Int]()
    val f = IO(future).futureLift.unsafeToFuture()

    Thread.sleep(100)
    assertEquals(f.value, None)

    future.complete(100)
    assertEquals(Await.result(f, 5.seconds), 100)
  }

  test("convert from async CompletableFuture; on failure; with Async[IO]") { _ =>
    val future = new CompletableFuture[Int]()
    val f = convertAsync(IO(future)).unsafeToFuture()

    Thread.sleep(100)
    assertEquals(f.value, None)

    val dummy = DummyException("dummy")
    future.completeExceptionally(dummy)

    val result = Await.ready(f, 5.seconds)
    assertEquals(result.value, Some(Failure(dummy)))
  }

  test("convert from async CompletableFuture; on failure; with Concurrent[IO]") { _ =>
    val future = new CompletableFuture[Int]()
    val f = convertAsync(IO(future)).unsafeToFuture()

    Thread.sleep(100)
    assertEquals(f.value, None)

    val dummy = DummyException("dummy")
    future.completeExceptionally(dummy)

    val result = Await.ready(f, 5.seconds)
    assertEquals(result.value, Some(Failure(dummy)))
  }

  test("CompletableFuture is cancelable via IO") { _ =>
    val future = new CompletableFuture[Int]()

    val (_, cancel) = convertAsync(IO(future)).unsafeToFutureCancelable()
    Thread.sleep(100)

    cancel()
    Thread.sleep(100)

    // Should be already completed (cancelled)
    assert(future.isCancelled || future.isDone, "future should be cancelled or done")
  }

  def convertAsync[F[_], A](fa: F[CompletableFuture[A]])(implicit F: Async[F]): F[A] =
    fa.futureLift
}
