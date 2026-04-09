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

import cats.effect.{Async, IO}
import cats.effect.unsafe.implicits.global
import minitest.TestSuite
import monix.catnap.syntax._
import monix.execution.exceptions.DummyException
import monix.execution.{Cancelable, CancelableFuture}
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.util.{Failure, Success}

object FutureLiftSuite extends TestSuite[Unit] {
  def setup() = ()
  def tearDown(env: Unit): Unit = ()

  private def unsafeRun[A](io: IO[A]): A =
    Await.result(io.unsafeToFuture(), 5.seconds)

  test("IO(future).futureLift") { _ =>
    var effect = 0
    val io = IO(Future { effect += 1; effect }).futureLift

    val r1 = unsafeRun(io)
    assertEquals(r1, 1)
    val r2 = unsafeRun(io)
    assertEquals(r2, 2)
  }

  test("IO(Future.successful).futureLift") { _ =>
    val io = IO(Future.successful(1)).futureLift

    assertEquals(unsafeRun(io), 1)
    assertEquals(unsafeRun(io), 1)
  }

  test("IO(Future.failed).futureLift") { _ =>
    val dummy = DummyException("dummy")
    val io = IO(Future.failed[Int](dummy)).futureLift

    val f1 = Await.ready(io.unsafeToFuture(), 5.seconds)
    assertEquals(f1.value, Some(Failure(dummy)))
    val f2 = Await.ready(io.unsafeToFuture(), 5.seconds)
    assertEquals(f2.value, Some(Failure(dummy)))
  }

  test("F.delay(future).futureLift for Async[F] data types") { _ =>
    import Overrides.asyncIO
    var effect = 0

    def mkInstance[F[_]: Async] =
      Async[F].delay(Future { effect += 1; effect }).futureLift

    val io = mkInstance[IO]
    assertEquals(unsafeRun(io), 1)
    assertEquals(unsafeRun(io), 2)
  }

  test("F.delay(Future.successful).futureLift for Async[F] data types") { _ =>
    import Overrides.asyncIO

    def mkInstance[F[_]: Async] =
      Async[F].delay(Future.successful(1)).futureLift

    val io = mkInstance[IO]
    assertEquals(unsafeRun(io), 1)
    assertEquals(unsafeRun(io), 1)
  }

  test("F.delay(Future.failed).futureLift for Async[F] data types") { _ =>
    import Overrides.asyncIO

    val dummy = DummyException("dummy")
    def mkInstance[F[_]: Async] =
      Async[F].delay(Future.failed[Int](dummy)).futureLift

    val io = mkInstance[IO]
    val f1 = Await.ready(io.unsafeToFuture(), 5.seconds)
    assertEquals(f1.value, Some(Failure(dummy)))
    val f2 = Await.ready(io.unsafeToFuture(), 5.seconds)
    assertEquals(f2.value, Some(Failure(dummy)))
  }

  test("F.delay(future).futureLift for Concurrent[F] data types") { _ =>
    var wasCanceled = 0
    val io = IO(CancelableFuture[Int](CancelableFuture.never, Cancelable { () =>
      wasCanceled += 1
    })).futureLift

    val (_, cancel) = io.unsafeToFutureCancelable()
    Thread.sleep(100)

    // Cancelling
    cancel()
    Thread.sleep(100)
    assertEquals(wasCanceled, 1)
  }

  test("FutureLift[F] instance for Concurrent[F] data types") { _ =>
    var wasCanceled = 0
    val source = Promise[Int]()
    val io = FutureLift[IO, CancelableFuture].apply(
      IO(
        CancelableFuture[Int](source.future, Cancelable { () =>
          wasCanceled += 1
        })
      ))

    val (_, cancel) = io.unsafeToFutureCancelable()
    Thread.sleep(100)

    // Cancelling
    cancel()
    Thread.sleep(100)
    assertEquals(wasCanceled, 1)

    val f2 = io.unsafeToFuture()
    source.success(1)
    val r2 = Await.result(f2, 5.seconds)
    assertEquals(r2, 1)
  }

  test("FutureLift[F] instance for Async[F] data types") { _ =>
    import Overrides.asyncIO

    var wasCanceled = 0
    val source = Promise[Int]()

    def mkInstance[F[_]](implicit F: Async[F]): F[Int] =
      FutureLift[F, CancelableFuture].apply(
        F.delay(
          CancelableFuture[Int](source.future, Cancelable { () =>
            wasCanceled += 1
          })
        ))

    val io = mkInstance[IO]
    val (_, cancel) = io.unsafeToFutureCancelable()
    Thread.sleep(100)

    // In CE3, Async[F] supports cancellation (unlike CE2 where it didn't)
    cancel()
    Thread.sleep(100)
    assertEquals(wasCanceled, 1)

    val source2 = Promise[Int]()
    val io2: IO[Int] = {
      implicit val F: Async[IO] = Overrides.asyncIO
      implicit val fl: FutureLift[IO, CancelableFuture] = FutureLift.scalaFutureLiftForAsync[IO, CancelableFuture]
      fl(IO.delay(CancelableFuture[Int](source2.future, Cancelable.empty)))
    }
    val f2 = io2.unsafeToFuture()
    source2.success(1)
    val r2 = Await.result(f2, 5.seconds)
    assertEquals(r2, 1)
  }
}
