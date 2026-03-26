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

package monix.execution.internal.atomic;

import monix.execution.internal.InternalApi;
import scala.util.control.NonFatal;
import java.lang.reflect.Field;

/**
 * INTERNAL API — used by queue builders to detect OpenJDK compatibility.
 *
 * Being internal it can always change between minor versions,
 * providing no backwards compatibility guarantees and is only public
 * because Java does not provide the capability of marking classes as
 * "internal" to a package and all its sub-packages.
 */
@InternalApi public final class UnsafeAccess {

  /**
   * Some platforms do not expose a `theUnsafe` private reference
   * to a `sun.misc.Unsafe` instance, but some libraries
   * (notably JCTools) depend on this.
   *
   * This reference is set to `true` in case `Unsafe.theUnsafe` exists,
   * or `false` otherwise.
   */
  public static final boolean IS_OPENJDK_COMPATIBLE;

  static {
    boolean isOpenJDKCompatible = false;

    try {
      Class<?> cls = Class.forName("sun.misc.Unsafe", true, UnsafeAccess.class.getClassLoader());
      Field field = cls.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      Object instance = field.get(null);
      isOpenJDKCompatible = instance != null;
    }
    catch (Exception ex) {
      if (!NonFatal.apply(ex)) throw new RuntimeException(ex);
    }
    finally {
      IS_OPENJDK_COMPATIBLE = isOpenJDKCompatible;
    }
  }
}
