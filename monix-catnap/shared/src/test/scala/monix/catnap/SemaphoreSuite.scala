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
import cats.implicits._
import minitest.TestSuite
import monix.execution.internal.Platform
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.util.{Random, Success}

object SemaphoreSuite extends TestSuite[Unit] {
  def setup() = ()
  def tearDown(env: Unit): Unit = ()

  /** Wait for the IO runtime to process pending work. */
  private def yieldRuntime(): Unit = Thread.sleep(100)

  test("simple greenLight") { _ =>
    val semaphore = Semaphore.unsafe[IO](provisioned = 4)
    val result = semaphore.withPermit(IO.cede *> IO(100)).unsafeRunSync()
    assertEquals(result, 100)
    assertEquals(semaphore.available.unsafeRunSync(), 4)
  }

  test("should back-pressure when full") { _ =>
    val semaphore = Semaphore.unsafe[IO](provisioned = 2)

    val p1 = Promise[Int]()
    val f1 = semaphore.withPermit(IO.fromFuture(IO.pure(p1.future))).unsafeToFuture()
    val p2 = Promise[Int]()
    val f2 = semaphore.withPermit(IO.fromFuture(IO.pure(p2.future))).unsafeToFuture()

    yieldRuntime()
    assertEquals(semaphore.available.unsafeRunSync(), 0)

    val f3 = semaphore.withPermit(IO(3)).unsafeToFuture()
    yieldRuntime()
    assertEquals(f3.value, None)
    assertEquals(semaphore.available.unsafeRunSync(), 0)

    p1.success(1); yieldRuntime()
    assertEquals(Await.result(f1, 5.seconds), 1)
    assertEquals(Await.result(f3, 5.seconds), 3)

    p2.success(2); yieldRuntime()
    assertEquals(Await.result(f2, 5.seconds), 2)
    assertEquals(semaphore.available.unsafeRunSync(), 2)
  }

  testAsync("real async test of many futures") { _ =>
    // Executing Futures on the global scheduler!
    import scala.concurrent.ExecutionContext.Implicits.global

    val semaphore = Semaphore.unsafe[IO](provisioned = 20)
    val count = if (Platform.isJVM) 10000 else 1000

    val futures = for (i <- 0 until count) yield semaphore.withPermit(IO.cede *> IO(i))
    val sum =
      futures.toList.parSequence.map(_.sum).unsafeToFuture()

    // Asynchronous result, to be handled by Minitest
    for (result <- sum) yield {
      assertEquals(result, count * (count - 1) / 2)
    }
  }

  test("await for release of all active and pending permits") { _ =>
    val semaphore = Semaphore.unsafe[IO](provisioned = 2)
    semaphore.acquire.unsafeRunSync()
    semaphore.acquire.unsafeRunSync()

    val p3 = semaphore.acquire.unsafeToFuture()
    yieldRuntime()
    assert(!p3.isCompleted, "!p3.isCompleted")
    val p4 = semaphore.acquire.unsafeToFuture()
    yieldRuntime()
    assert(!p4.isCompleted, "!p4.isCompleted")

    val all1 = semaphore.awaitAvailable(2).unsafeToFuture()
    yieldRuntime()
    assert(!all1.isCompleted, "!all1.isCompleted")

    semaphore.release.unsafeRunSync(); yieldRuntime()
    assert(!all1.isCompleted, "!all1.isCompleted")
    semaphore.release.unsafeRunSync(); yieldRuntime()
    assert(!all1.isCompleted, "!all1.isCompleted")
    semaphore.release.unsafeRunSync(); yieldRuntime()
    assert(!all1.isCompleted, "!all1.isCompleted")
    semaphore.release.unsafeRunSync(); yieldRuntime()
    assert(all1.isCompleted, "all1.isCompleted")

    // REDO
    semaphore.acquire.unsafeRunSync()
    val all2 = semaphore.awaitAvailable(2).unsafeToFuture()
    yieldRuntime(); assert(!all2.isCompleted, "!all2.isCompleted")
    semaphore.release.unsafeRunSync(); yieldRuntime()
    assert(all2.isCompleted, "all2.isCompleted")

    // Already completed
    val all3 = semaphore.awaitAvailable(2).unsafeToFuture(); yieldRuntime()
    assert(all3.isCompleted, "all3.isCompleted")
  }

  test("acquire is cancelable") { _ =>
    val semaphore = Semaphore.unsafe[IO](provisioned = 2)

    semaphore.acquire.unsafeRunSync()
    semaphore.acquire.unsafeRunSync()

    val (_, cancel) = semaphore.acquire.unsafeToFutureCancelable()
    yieldRuntime()
    assertEquals(semaphore.available.unsafeRunSync(), 0)

    cancel(); yieldRuntime()
    semaphore.release.unsafeRunSync()
    assertEquals(semaphore.available.unsafeRunSync(), 1)
    semaphore.release.unsafeRunSync()
    assertEquals(semaphore.available.unsafeRunSync(), 2)
  }

  testAsync("withPermitN / awaitAvailable concurrent test") { _ =>
    // Executing Futures on the global scheduler!
    import scala.concurrent.ExecutionContext.Implicits.global

    val task = repeatTest(10) {
      val available = 6L
      val semaphore = Semaphore.unsafe[IO](provisioned = available)
      val count = if (Platform.isJVM) 10000 else 50
      val allReleased = Promise[Unit]()

      val task = semaphore.withPermit(IO.defer {
        allReleased.completeWith(semaphore.awaitAvailable(available).unsafeToFuture())

        val futures = for (i <- 0 until count) yield {
          semaphore.withPermitN(Math.floorMod(Random.nextInt(), 3).toLong + 1) {
            IO(1).map { x =>
              assert(!allReleased.isCompleted, s"!allReleased.isCompleted (index $i)")
              x
            }
          }
        }
        futures.toList.parSequence.map { x =>
          x.sum
        }
      })

      for (r <- task; _ <- IO.fromFuture(IO.pure(allReleased.future))) yield {
        assertEquals(r, count)
        assertEquals(semaphore.available.unsafeRunSync(), available)
      }
    }
    task.unsafeToFuture()
  }

  test("withPermitN has FIFO priority") { _ =>
    val sem = Semaphore.unsafe[IO](provisioned = 0)

    val f1 = sem.withPermitN(3)(IO(1 + 1)).unsafeToFuture()
    yieldRuntime()
    assertEquals(f1.value, None)
    val f2 = sem.withPermitN(4)(IO(1 + 1)).unsafeToFuture()
    yieldRuntime()
    assertEquals(f2.value, None)

    sem.releaseN(2).unsafeRunSync(); yieldRuntime()
    assertEquals(f1.value, None)
    assertEquals(f2.value, None)

    sem.releaseN(1).unsafeRunSync(); yieldRuntime()
    assertEquals(Await.result(f1, 5.seconds), 2)
    assertEquals(f2.value, None)

    sem.releaseN(1).unsafeRunSync(); yieldRuntime()
    assertEquals(Await.result(f2, 5.seconds), 2)
  }

  test("withPermitN is cancelable (1)") { _ =>
    val sem = Semaphore.unsafe[IO](provisioned = 0)

    val task = for {
      fib1 <- sem.withPermitN(3)(IO(1 + 1)).start
      _    <- IO.sleep(200.millis)
      _    <- IO(assertEquals(sem.count.unsafeRunSync(), -3L))
      _    <- fib1.cancel
      _    <- IO(assertEquals(sem.count.unsafeRunSync(), 0L))
    } yield ()

    assertEquals(task.unsafeRunTimed(5.seconds), Some(()))
  }

  test("withPermitN is cancelable (2)") { _ =>
    val sem = Semaphore.unsafe[IO](provisioned = 1)

    val task = for {
      fib1 <- sem.withPermitN(3)(IO(1 + 1)).start
      fib2 <- sem.withPermitN(3)(IO(1 + 1)).start
      _    <- IO.sleep(100.millis)
      _    <- IO(assertEquals(sem.count.unsafeRunSync(), -5L))
      _    <- sem.releaseN(1)
      _    <- IO(assertEquals(sem.count.unsafeRunSync(), -4L))
      _    <- fib1.cancel
      _    <- IO.sleep(100.millis)
      _    <- IO(assertEquals(sem.count.unsafeRunSync(), -1L))
      _    <- sem.releaseN(1)
      r2   <- fib2.joinWithNever
    } yield r2

    assertEquals(task.unsafeRunTimed(10.seconds), Some(2))
  }

  def repeatTest(n: Int)(f: => IO[Unit]): IO[Unit] =
    if (n > 0) f.flatMap(_ => repeatTest(n - 1)(f))
    else IO.unit
}
