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

package monix.execution.atomic

import monix.execution.atomic.PaddingStrategy.NoPadding
import monix.execution.internal.atomic.{BoxedObject, Factory}

/** Atomic references wrapping `AnyRef` values.
  *
  * @tparam A is forced to be an `AnyRef` because the equality test is
  *         by reference and not by value.
  */
final class AtomicAny[A <: AnyRef] private (private[this] val ref: BoxedObject) extends Atomic[A] {
  def get(): A = ref.volatileGet().asInstanceOf[A]
  def set(update: A): Unit = ref.volatileSet(update)

  def compareAndSet(expect: A, update: A): Boolean =
    ref.compareAndSet(expect, update)

  def getAndSet(update: A): A =
    ref.getAndSet(update).asInstanceOf[A]

  def lazySet(update: A): Unit =
    ref.lazySet(update)
}

/** @define createDesc Constructs an [[AtomicAny]] reference, allowing
  *         for fine-tuning of the created instance.
  *
  *         A [[PaddingStrategy]] can be provided in order to counter
  *         the "false sharing" problem.
  *
  *         Note that for ''Scala.js'' we aren't applying any padding,
  *         as it doesn't make much sense, since Javascript execution
  *         is single threaded, but this builder is provided for
  *         syntax compatibility anyway across the JVM and Javascript
  *         and we never know how Javascript engines will evolve.
  */
object AtomicAny {
  /** Builds an [[AtomicAny]] reference.
    *
    * @param initialValue is the initial value with which to initialize the atomic
    */
  def apply[A <: AnyRef](initialValue: A): AtomicAny[A] =
    withPadding(initialValue, NoPadding)

  /** $createDesc
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def withPadding[A <: AnyRef](initialValue: A, padding: PaddingStrategy): AtomicAny[A] =
    create(initialValue, padding)

  /** $createDesc
    *
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def create[A <: AnyRef](initialValue: A, padding: PaddingStrategy): AtomicAny[A] = {
    new AtomicAny(
      Factory.newBoxedObject(
        initialValue,
        boxStrategyToPaddingStrategy(padding),
      ))
  }

  @deprecated("Use create(initialValue, padding) instead", "3.4.0-avs6")
  def create[A <: AnyRef](initialValue: A, padding: PaddingStrategy, allowPlatformIntrinsics: Boolean): AtomicAny[A] = create(initialValue, padding)

  /** $createDesc
    *
    * This builder guarantees to construct a safe atomic reference that
    * is equivalent to [[create]]. Kept for binary compatibility.
    *
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def safe[A <: AnyRef](initialValue: A, padding: PaddingStrategy): AtomicAny[A] =
    new AtomicAny(
      Factory.newBoxedObject(
        initialValue,
        boxStrategyToPaddingStrategy(padding),
      ))
}
