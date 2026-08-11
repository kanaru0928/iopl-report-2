package lr0pg

class NFA[S, Q](
    val alphabets: Set[S],
    val states: Set[Q],
    val startState: Q,
    val acceptStates: Set[Q],
    transitions: Map[(Q, Option[S]), Set[Q]]
) {
  def transition(state: Q, input: Option[S]): Set[Q] = {
    transitions.getOrElse((state, input), Set.empty)
  }

  override def toString: String = {
    val transitionStrings = transitions.map {
      case ((state, input), nextStates) =>
        val inputStr = input match {
          case Some(symbol) => symbol.toString
          case None         => "ε"
        }
        s"$state --$inputStr--> ${nextStates.mkString(", ")}"
    }
    s"NFA(\n  States: ${states.mkString(", ")},\n  Start State: $startState,\n  Accept States: ${acceptStates.mkString(", ")},\n  Transitions:\n    ${transitionStrings.mkString("\n    ")}\n)"
  }
}
