/*
 * Copyright (c) 2014-2022 Monix Contributors.
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

package monix.reactive.internal.operators

import cats.effect.ExitCase
import monix.eval.Task
import monix.execution.exceptions.DummyException
import monix.execution.schedulers.TestScheduler
import monix.reactive.{ BaseTestSuite, Observable }

import scala.concurrent.duration._

object UpstreamCancellationSuite extends BaseTestSuite {

  private val dummy = DummyException("dummy")

  // Each row pairs a label with a transformation that introduces a failure
  // downstream of the (optional) `mergeMap` under test. In both variants
  // the contract is the same: the `Stop` ack returned by the failing
  // operator must travel back up the ack chain so that
  // `GuaranteeSubscriber.detectStopOrFailure` fires `ExitCase.Canceled`.
  //
  // Baseline (no mergeMap): the transformation is applied directly to a
  // `guaranteeCase`-guarded source. The contract holds today.
  //
  // mergeMap variant: an identity `mergeMap` is inserted between source
  // and transformation. mergeMap must propagate the `Stop` upstream just
  // like the baseline. Today it does not — `MergeMapObservable` decouples
  // its producer side (inner subscribers calling `subscriberB.onNext`)
  // from its consumer side (`BufferedSubscriber.fastLoop` draining to
  // `downstream`); when `fastLoop` sees `Stop` it sets
  // `downstreamIsComplete = true` and exits without notifying mergeMap's
  // outer subscriber or calling `cancelUpstream()`; the outer `onNext`
  // returns `Continue` unconditionally; and `syncOnStopOrFailure` only
  // fires on a subsequent `subscriberB.onNext`. Until that is fixed these
  // rows fail; s
  private val downstreamFailures: List[(String, Observable[Long] => Observable[Long])] = List(
    "collect throws" -> (_.collect { case _ => throw dummy }),
    "collectWhile throws" -> (_.collectWhile { case _ => throw dummy }),
    "collectWhile throws" -> (_.collectWhile { case _ => throw dummy }),
    "doOnNext task fails" -> (_.doOnNext(_ => Task.raiseError(dummy))),
    "doOnNextAck task fails" -> (_.doOnNextAck((_, _) => Task.raiseError(dummy))),
    "dropWhile p throws" -> (_.dropWhile(_ => throw dummy)),
    "filterEval task fails" -> (_.filterEval(_ => Task.raiseError(dummy))),
    "filter throws" -> (_.filter(_ => throw dummy)),
    "foldLeft op throws" -> (_.foldLeft(0L)((_, _) => throw dummy)),
    "foldWhileLeft op throws" -> (_.foldWhileLeft(0L)((_, _) => throw dummy)),
    "mapEval task fails" -> (_.mapEval(_ => Task.raiseError(dummy))),
    "map throws" -> (_.map(_ => throw dummy)),
    "mapAccumulate op throws" -> (_.mapAccumulate(0L)((_, _) => throw dummy)),
    "mapParallelOrdered task fails" -> (_.mapParallelOrdered(2)(_ => Task.raiseError(dummy))),
    "mapParallelUnordered task fails" -> (_.mapParallelUnordered(2)(_ => Task.raiseError(dummy))),
    "scanEval op fails" -> (_.scanEval(Task.pure(0L))((_, _) => Task.raiseError(dummy))),
    "scan op throws" -> (_.scan(0L)((_, _) => throw dummy)),
    "takeWhile p throws" -> (_.takeWhile(_ => throw dummy)),
  )

  for ((label, downstream) <- downstreamFailures) {
    test(s"no additional operator: guaranteeCase observes Canceled when downstream $label") { implicit s =>
      assertCancelObserved(downstream)
    }

    test(s"with mergeMap: guaranteeCase observes Canceled when downstream $label") { implicit s =>
      assertCancelObserved(_.mergeMap(Observable.now).transform(downstream))
    }

    test(s"with concatMap: guaranteeCase observes Canceled when downstream $label") { implicit s =>
      assertCancelObserved(_.concatMap(Observable.now).transform(downstream))
    }
  }

  private def assertCancelObserved(
    buildDownstream: Observable[Long] => Observable[Long],
  )(implicit scheduler: TestScheduler): Unit = {
    var exitCase: ExitCase[Throwable] = null

    val source = Observable
      .range(0L, 1000L)
      .delayOnNext(1.second)
      .guaranteeCase(ec => Task { exitCase = ec })

    val future = source.transform(buildDownstream).completedL.runToFuture

    scheduler.tick(1.second)

    assertEquals(future.value.map(_.isFailure), Some(true))
    assertEquals(exitCase, ExitCase.Canceled)
  }
}
