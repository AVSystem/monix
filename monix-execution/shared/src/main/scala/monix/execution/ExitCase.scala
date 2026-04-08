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

package monix.execution

/** An ADT representing the exit condition of an effectful computation.
  *
  * This is a Monix-local replacement for the `cats.effect.ExitCase` that
  * existed in Cats Effect 2. In Cats Effect 3, the equivalent is
  * `cats.effect.Outcome[F, E, A]`, but that is parameterized on the
  * effect type, which is too complex for Monix's Task/Observable APIs.
  */
sealed trait ExitCase[+E] extends Product with Serializable

object ExitCase {
  /** The task completed successfully. */
  case object Completed extends ExitCase[Nothing]

  /** The task completed with an error. */
  final case class Error[+E](e: E) extends ExitCase[E]

  /** The task was canceled. */
  case object Canceled extends ExitCase[Nothing]
}
