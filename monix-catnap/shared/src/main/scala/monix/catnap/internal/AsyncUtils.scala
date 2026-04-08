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

package monix.catnap.internal

import cats.effect.Async
import cats.syntax.all._

private[monix] object AsyncUtils {
  /** Describing the `cancelable` builder for any `Async` data type,
    * using CE3's `async` which supports optional cancellation tokens.
    *
    * `k` registers the callback and returns an `F[Unit]` cancel token.
    * We must NOT evaluate (flatMap/map) the cancel token — just pass it
    * to `F.async` as `Some(cancelToken)` so it is only evaluated on
    * actual cancellation.
    */
  def cancelable[F[_], A](k: (Either[Throwable, A] => Unit) => F[Unit])(implicit F: Async[F]): F[A] =
    F.async[A] { cb =>
      F.delay {
        val cancelToken = k(cb)
        Some(cancelToken)
      }
    }
}
