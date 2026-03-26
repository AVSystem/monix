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

/** Atomic references wrapping `Short` values.
  *
  * Note that the equality test in `compareAndSet` is value based,
  * since `Short` is a primitive.
  */
final class AtomicShort private (private[this] val ref: BoxedInt) extends AtomicNumber[Short] {
  private[this] val mask = 255 + 255 * 256

  def get(): Short =
    (ref.volatileGet() & mask).asInstanceOf[Short]

  def set(update: Short): Unit =
    ref.volatileSet(update.asInstanceOf[Int])

  def lazySet(update: Short): Unit =
    ref.lazySet(update.asInstanceOf[Int])

  def compareAndSet(expect: Short, update: Short): Boolean =
    ref.compareAndSet(expect.asInstanceOf[Int], update.asInstanceOf[Int])

  def getAndSet(update: Short): Short =
    (ref.getAndSet(update.asInstanceOf[Int]) & mask).asInstanceOf[Short]

  def increment(v: Int = 1): Unit = {
    ref.getAndAdd(v)
    ()
  }

  def add(v: Short): Unit = {
    ref.getAndAdd(v.asInstanceOf[Int])
    ()
  }

  def incrementAndGet(v: Int = 1): Short =
    ((ref.getAndAdd(v) + v) & mask).asInstanceOf[Short]

  def addAndGet(v: Short): Short =
    ((ref.getAndAdd(v.asInstanceOf[Int]) + v) & mask).asInstanceOf[Short]

  def getAndIncrement(v: Int = 1): Short =
    (ref.getAndAdd(v) & mask).asInstanceOf[Short]

  def getAndAdd(v: Short): Short =
    (ref.getAndAdd(v.asInstanceOf[Int]) & mask).asInstanceOf[Short]

  def subtract(v: Short): Unit = {
    ref.getAndAdd(-v.asInstanceOf[Int])
    ()
  }

  def subtractAndGet(v: Short): Short =
    ((ref.getAndAdd(-v.asInstanceOf[Int]) - v) & mask).asInstanceOf[Short]

  def getAndSubtract(v: Short): Short =
    (ref.getAndAdd(-v.asInstanceOf[Int]) & mask).asInstanceOf[Short]

  def decrement(v: Int = 1): Unit = increment(-v)
  def decrementAndGet(v: Int = 1): Short = incrementAndGet(-v)
  def getAndDecrement(v: Int = 1): Short = getAndIncrement(-v)
}

/** @define createDesc Constructs an [[AtomicShort]] reference, allowing
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
object AtomicShort {
  /** Builds an [[AtomicShort]] reference.
    *
    * @param initialValue is the initial value with which to initialize the atomic
    */
  def apply(initialValue: Short): AtomicShort =
    withPadding(initialValue, NoPadding)

  /** $createDesc
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def withPadding(initialValue: Short, padding: PaddingStrategy): AtomicShort =
    create(initialValue, padding)

  /** $createDesc
    *
    *
    * @param initialValue is the initial value with which to initialize the atomic
    * @param padding is the [[PaddingStrategy]] to apply
    */
  def create(initialValue: Short, padding: PaddingStrategy): AtomicShort = {
    new AtomicShort(
      Factory.newBoxedInt(
        initialValue.toInt,
        boxStrategyToPaddingStrategy(padding),
      )
    )
  }

  @deprecated("Use create(initialValue, padding) instead", "3.4.0-avs6")
  def create(initialValue: Short, padding: PaddingStrategy, allowPlatformIntrinsics: Boolean): AtomicShort =
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
  def safe(initialValue: Short, padding: PaddingStrategy): AtomicShort = {
    new AtomicShort(
      Factory.newBoxedInt(
        initialValue.toInt,
        boxStrategyToPaddingStrategy(padding),
      )
    )
  }
}
