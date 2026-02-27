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

import minitest.SimpleTestSuite
import monix.execution.atomic.PaddingStrategy._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class ConcurrentAtomicNumberSuite[A, R <: AtomicNumber[A]](
  builder: AtomicBuilder[A, R],
  strategy: PaddingStrategy,
  value: A,
  nan1: Option[A],
  maxValue: A,
  minValue: A,
)(implicit ev: Numeric[A])
  extends SimpleTestSuite {

  def Atomic(initial: A): R = builder.buildInstance(initial, strategy)

  val two = ev.plus(ev.one, ev.one)

  test("should perform concurrent compareAndSet") {
    val r = Atomic(ev.zero)
    val futures =
      for (i <- 0 until 5) yield Future {
        for (j <- 0 until 100)
          r.increment()
      }

    val f = Future.sequence(futures)
    Await.result(f, 30.seconds)
    assert(r.get() == ev.fromInt(500))
  }

  test("should perform concurrent getAndSet") {
    val r = Atomic(ev.zero)
    val futures =
      for (i <- 0 until 5) yield Future {
        for (j <- 0 until 100)
          r.getAndSet(ev.fromInt(j + 1))
      }

    val f = Future.sequence(futures)
    Await.result(f, 30.seconds)
    assert(r.get() == ev.fromInt(100))
  }

  test("should perform concurrent increment") {
    val r = Atomic(ev.zero)
    val futures =
      for (i <- 0 until 5) yield Future {
        for (j <- 0 until 100)
          r.increment()
      }

    val f = Future.sequence(futures)
    Await.result(f, 30.seconds)
    assert(r.get() == ev.fromInt(500))
  }

  test("should perform concurrent incrementAndGet") {
    val r = Atomic(ev.zero)
    val futures =
      for (i <- 0 until 5) yield Future {
        for (j <- 0 until 100)
          r.incrementAndGet()
      }

    val f = Future.sequence(futures)
    Await.result(f, 30.seconds)
    assert(r.get() == ev.fromInt(500))
  }

  test("should perform concurrent getAndIncrement") {
    val r = Atomic(ev.zero)
    val futures =
      for (i <- 0 until 5) yield Future {
        for (j <- 0 until 100)
          r.getAndIncrement()
      }

    val f = Future.sequence(futures)
    Await.result(f, 30.seconds)
    assert(r.get() == ev.fromInt(500))
  }
}

//-- NoPadding (Java 8)

object ConcurrentAtomicNumberDoubleNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    NoPadding,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    NoPadding,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    NoPadding,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    NoPadding,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    NoPadding,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    NoPadding,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    NoPadding,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyNoPaddingSuite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    NoPadding,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//--Left64 (Java 8)

object ConcurrentAtomicNumberDoubleLeft64Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Left64,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeft64Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Left64,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeft64Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Left64,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeft64Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Left64,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeft64Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Left64,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeft64Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Left64,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeft64Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Left64,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeft64Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Left64,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- Right64 (Java 8)

object ConcurrentAtomicNumberDoubleRight64Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Right64,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatRight64Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Right64,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongRight64Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Right64,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntRight64Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Right64,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortRight64Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Right64,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteRight64Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Right64,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharRight64Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Right64,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyRight64Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Right64,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- LeftRight128 (Java 8)

object ConcurrentAtomicNumberDoubleLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    LeftRight128,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    LeftRight128,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    LeftRight128,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    LeftRight128,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    LeftRight128,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    LeftRight128,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    LeftRight128,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeftRight128Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    LeftRight128,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//--Left128 (Java 8)

object ConcurrentAtomicNumberDoubleLeft128Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Left128,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeft128Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Left128,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeft128Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Left128,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeft128Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Left128,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeft128Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Left128,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeft128Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Left128,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeft128Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Left128,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeft128Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Left128,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- Right128 (Java 8)

object ConcurrentAtomicNumberDoubleRight128Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Right128,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatRight128Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Right128,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongRight128Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Right128,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntRight128Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Right128,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortRight128Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Right128,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteRight128Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Right128,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharRight128Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Right128,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyRight128Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Right128,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- LeftRight256 (Java 8)

object ConcurrentAtomicNumberDoubleLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    LeftRight256,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    LeftRight256,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    LeftRight256,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    LeftRight256,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    LeftRight256,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    LeftRight256,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    LeftRight256,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeftRight256Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    LeftRight256,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

// ------------ Java 7

//-- NoPadding (Java 7)

object ConcurrentAtomicNumberDoubleNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    NoPadding,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    NoPadding,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    NoPadding,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    NoPadding,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    NoPadding,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    NoPadding,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    NoPadding,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyNoPaddingJava7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    NoPadding,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//--Left64 (Java 7)

object ConcurrentAtomicNumberDoubleLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Left64,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Left64,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Left64,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Left64,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Left64,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Left64,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Left64,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeft64Java7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Left64,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- Right64 (Java 7)

object ConcurrentAtomicNumberDoubleRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Right64,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Right64,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Right64,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Right64,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Right64,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Right64,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Right64,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyRight64Java7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Right64,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- LeftRight128 (Java 7)

object ConcurrentAtomicNumberDoubleLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    LeftRight128,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    LeftRight128,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    LeftRight128,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    LeftRight128,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    LeftRight128,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    LeftRight128,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    LeftRight128,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeftRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    LeftRight128,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//--Left128 (Java 7)

object ConcurrentAtomicNumberDoubleLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Left128,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Left128,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Left128,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Left128,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Left128,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Left128,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Left128,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeft128Java7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Left128,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- Right128 (Java 7)

object ConcurrentAtomicNumberDoubleRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    Right128,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    Right128,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    Right128,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    Right128,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    Right128,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    Right128,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    Right128,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyRight128Java7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    Right128,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )

//-- LeftRight256 (Java 7)

object ConcurrentAtomicNumberDoubleLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Double, AtomicDouble](
    Atomic.builderFor(0.0),
    LeftRight256,
    17.23,
    Some(Double.NaN),
    Double.MaxValue,
    Double.MinValue,
  )

object ConcurrentAtomicNumberFloatLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Float, AtomicFloat](
    Atomic.builderFor(0.0f),
    LeftRight256,
    17.23f,
    Some(Float.NaN),
    Float.MaxValue,
    Float.MinValue,
  )

object ConcurrentAtomicNumberLongLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Long, AtomicLong](
    Atomic.builderFor(0L),
    LeftRight256,
    -782L,
    None,
    Long.MaxValue,
    Long.MinValue,
  )

object ConcurrentAtomicNumberIntLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Int, AtomicInt](
    Atomic.builderFor(0),
    LeftRight256,
    782,
    None,
    Int.MaxValue,
    Int.MinValue,
  )

object ConcurrentAtomicNumberShortLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Short, AtomicShort](
    Atomic.builderFor(0.toShort),
    LeftRight256,
    782.toShort,
    None,
    Short.MaxValue,
    Short.MinValue,
  )

object ConcurrentAtomicNumberByteLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Byte, AtomicByte](
    Atomic.builderFor(0.toByte),
    LeftRight256,
    782.toByte,
    None,
    Byte.MaxValue,
    Byte.MinValue,
  )

object ConcurrentAtomicNumberCharLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[Char, AtomicChar](
    Atomic.builderFor(0.toChar),
    LeftRight256,
    782.toChar,
    None,
    Char.MaxValue,
    Char.MinValue,
  )

object ConcurrentAtomicNumberNumberAnyLeftRight256Java7Suite
  extends ConcurrentAtomicNumberSuite[BigInt, AtomicNumberAny[BigInt]](
    Atomic.builderFor(BigInt(0)),
    LeftRight256,
    BigInt(Int.MaxValue),
    None,
    BigInt(Long.MaxValue),
    BigInt(Long.MinValue),
  )
