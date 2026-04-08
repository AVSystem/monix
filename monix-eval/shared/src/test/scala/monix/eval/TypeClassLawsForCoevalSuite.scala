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

import cats.effect.laws.SyncTests
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline.{CoflatMapTests, SemigroupKTests}

object TypeClassLawsForCoevalSuite extends BaseLawsSuite {

  implicit val arbSyncType: org.scalacheck.Arbitrary[cats.effect.kernel.Sync.Type] = {
    import cats.effect.kernel.Sync.Type._
    org.scalacheck.Arbitrary(org.scalacheck.Gen.oneOf(
      Delay, Blocking, InterruptibleOnce, InterruptibleMany
    ))
  }

  // TODO: Re-enable once CE3 law test infrastructure is fully set up
  // checkAll("Sync[Coeval]", SyncTests[Coeval].sync[Int, Int, Int])

  checkAll("CoflatMap[Coeval]", CoflatMapTests[Coeval].coflatMap[Int, Int, Int])

  checkAll("Monoid[Coeval[Int]]", MonoidTests[Coeval[Int]].monoid)

  checkAll("SemigroupK[Coeval[Int]]", SemigroupKTests[Coeval].semigroupK[Int])
}
