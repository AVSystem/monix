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
import cats.effect.IO
import cats.effect.unsafe.implicits.{global => ioRuntime}
import monix.execution.exceptions.DummyException

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TaskLiftSuite extends BaseTestSuite {

  test("task.to[Task]") { _ =>
    val task = Task(1)
    val conv = task.to[Task]
    assertEquals(task, conv)
  }

  test("task.to[IO]") { implicit s =>
    val task = Task(1)
    val io = task.to[IO]
    val f = io.unsafeToFuture()

    assertEquals(Await.result(f, 5.seconds), 1)
  }

  test("task.to[IO] for errors") { implicit s =>
    val dummy = DummyException("dummy")
    val task = Task.raiseError(dummy)
    val io = task.to[IO]
    val f = Await.ready(io.unsafeToFuture(), 5.seconds)

    assertEquals(f.value, Some(Failure(dummy)))
  }

  // Tests for task.to[Effect] and task.to[ConcurrentEffect] removed —
  // those CE2 type classes no longer exist in Cats Effect 3.
}
