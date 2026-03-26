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

package monix.execution.internal

import java.lang.invoke.{MethodHandle, MethodHandles, MethodType}

//todo: remove when JDK < 19 support dropped
private[monix] object ThreadCompat {
  private val ThreadIdHandle: MethodHandle =
    try
      // JDK >= 19
      MethodHandles.lookup.findVirtual(classOf[Thread], "threadId", MethodType.methodType(classOf[Long]))
    catch {
      case _: NoSuchMethodException | _: IllegalAccessException =>
        try
          // JDK < 19
          MethodHandles.lookup.findVirtual(classOf[Thread], "getId", MethodType.methodType(classOf[Long]))
        catch {
          case ex: Exception =>
            throw new RuntimeException(ex)
        }
    }

  implicit final class ThreadCompatOps(private val thread: Thread) extends AnyVal {
    def threadIdCompat: Long = ThreadIdHandle.invokeExact(thread).asInstanceOf[Long]
  }
}
