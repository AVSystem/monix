import com.typesafe.tools.mima.core._

object MimaFilters {
  // MiMa filters cleared for Monix 4.x (no binary compatibility with 3.x)
  lazy val changesFor_3_2_0: Seq[ProblemFilter] = Seq.empty
  lazy val changesFor_3_0_1: Seq[ProblemFilter] = Seq.empty
  lazy val changesFor_3_3_0: Seq[ProblemFilter] = Seq.empty
  lazy val changesFor_3_4_0: Seq[ProblemFilter] = Seq.empty
  lazy val changesFor_avs: Seq[ProblemFilter] = Seq.empty
}
