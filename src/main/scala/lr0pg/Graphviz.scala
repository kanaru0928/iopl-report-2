package lr0pg

object Graphviz {
  private def escape(label: String): String =
    label.replace("\\", "\\\\").replace("\"", "\\\"")

  def digraph[Q](
      name: String,
      states: Set[Q],
      startState: Q,
      acceptStates: Set[Q],
      edges: Seq[(Q, String, Q)]
  ): String = {
    val allStates = states ++ edges.map(_._3) + startState
    val ids = allStates.zipWithIndex.map { case (state, index) =>
      state -> s"q$index"
    }.toMap

    val nodeLines = allStates.toSeq.map { state =>
      val shape = if (acceptStates.contains(state)) "doublecircle" else "circle"
      s"""  ${ids(state)} [label="${escape(state.toString)}", shape=$shape];"""
    }
    val edgeLines = edges.map { case (from, label, to) =>
      s"""  ${ids(from)} -> ${ids(to)} [label="${escape(label)}"];"""
    }
    val bodyLines = Seq(
      "  rankdir=LR;",
      "  __start [shape=point];",
      s"  __start -> ${ids(startState)};"
    ) ++ nodeLines ++ edgeLines

    s"digraph $name {\n${bodyLines.mkString("\n")}\n}\n"
  }
}
