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
import monix.execution.internal.atomic.{ BoxedInt, Factory }

/** Atomic references wrapping `Char` values.
  *
  * Note that the equality test in `compareAndSet` is value based,
  * since `Char` is a primitive.
  */
final class AtomicChar private (private[this] val ref: BoxedInt) extends AtomicNumber[Char] {
  private[this] val mask = 255 + 255 * 256

  def get(): Char =
    (ref.volatileGet() & mask).asInstanceOf[Char]

  def set(update: Char): Unit =
    ref.volatileSet(update.asInstanceOf[Int])

  def lazySet(update: Char): Unit =
    ref.lazySet(update.asInstanceOf[Int])

  def compareAndSet(expect: Char, update: Char): Boolean =
    ref.compareAndSet(expect.asInstanceOf[Int], update.asInstanceOf[Int])

  def getAndSet(update: Char): Char =
    (ref.getAndSet(update.asInstanceOf[Int]) & mask).asInstanceOf[Char]

  def increment(v: Int = 1): Unit = {
    ref.getAndAdd(v)
    ()
  }

  def add(v: Char): Unit = {
    ref.getAndAdd(v.asInstanceOf[Int])
    ()
  }

  def incrementAndGet(v: Int = 1): Char =
    ((ref.getAndAdd(v) + v) & mask).asInstanceOf[Char]

  def addAndGet(v: Char): Char =
    ((ref.getAndAdd(v.asInstanceOf[Int]) + v) & mask).asInstanceOf[Char]

  def getAndIncrement(v: Int = 1): Char =
    (ref.getAndAdd(v.asInstanceOf[Int]) & mask).asInstanceOf[Char]

  def getAndAdd(v: Char): Char =
    (ref.getAndAdd(v.asInstanceOf[Int]) & mask).asInstanceOf[Char]

  def subtract(v: Char): Unit = {
    ref.getAndAdd(-v.asInstanceOf[Int])
    ()
  }

  def subtractAndGet(v: Char): Char =
    ((ref.getAndAdd(-v.asInstanceOf[Int]) - v) & mask).asInstanceOf[Char]

  def getAndSubtract(v: Char): Char =
    (ref.getAndAdd(-v.asInstanceOf[Int]) & mask).asInstanceOf[Char]

  def decrement(v: Int = 1): Unit = increment(-v)
  def decrementAndGet(v: Int = 1): Char = incrementAndGet(-v)
  def getAndDecrement(v: Int = 1): Char = getAndIncrement(-v)
}

/** @define createDesc Constructs an [[AtomicChar]] reference, allowing
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
object AtomicChar {
  /** Builds an [[AtomicChar]] reference.
    *
    * @param initialValue is the initial value with which to initialize the atomic
    */
  def apply(initialValue: Char): AtomicChar =
    withPadding(initialValue, NoPadding)

  /** $createDesc
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def withPadding(initialValue: Char, padding: PaddingStrategy): AtomicChar =
    create(initialValue, padding)

  /** $createDesc
    *
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def create(initialValue: Char, padding: PaddingStrategy): AtomicChar = {
    new AtomicChar(
      Factory.newBoxedInt(
        initialValue.toInt,
        boxStrategyToPaddingStrategy(padding),
      )
    )
  }

  @deprecated("Use create(initialValue, padding) instead", "3.4.0-avs6")
  def create(initialValue: Char, padding: PaddingStrategy, allowPlatformIntrinsics: Boolean): AtomicChar =
    create(initialValue, padding)

  /** $createDesc
    *
    * This builder guarantees to construct a safe atomic reference that
    * is equivalent to [[create]]. Kept for binary compatibility.
    *
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def safe(initialValue: Char, padding: PaddingStrategy): AtomicChar = {
    new AtomicChar(
      Factory.newBoxedInt(
        initialValue.toInt,
        boxStrategyToPaddingStrategy(padding),
      )
    )
  }
}
