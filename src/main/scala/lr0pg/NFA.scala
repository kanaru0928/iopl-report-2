package lr0pg

class NFA[S, Q](
    val alphabets: Set[S],
    val states: Set[Q],
    val startState: Q,
    val acceptStates: Set[Q],
    transitions: Map[(Q, Option[S]), Set[Q]]
) {
  private var epsilonClosureCache: Map[Q, Set[Q]] = Map.empty

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

  def toDot: String = Graphviz.digraph(
    "NFA",
    states,
    startState,
    acceptStates,
    transitions.toSeq.flatMap { case ((state, input), nextStates) =>
      val label = input.map(_.toString).getOrElse("ε")
      nextStates.map(nextState => (state, label, nextState))
    }
  )

  def epsilonClosure(state: Q): Set[Q] = {
    epsilonClosureCache.getOrElse(
      state, {
        transition(state, None).foldLeft(Set(state)) { (closure, nextState) =>
          closure ++ epsilonClosure(nextState)
        }
      }
    )
  }

  def purgeEpsilonTransitions: NFA[S, Q] = {
    val newTransitions: Map[(Q, Option[S]), Set[Q]] = states.flatMap(state => {
      val closure = epsilonClosure(state)
      alphabets.flatMap(alphabet => {
        val nextStates = closure.flatMap(s => transition(s, Some(alphabet)))
        val nextStatesWithClosure = nextStates.flatMap(s => epsilonClosure(s))
        if (nextStatesWithClosure.nonEmpty) {
          Some((state, Some(alphabet)) -> nextStatesWithClosure)
        } else {
          None
        }
      })
    }).toMap

    val newAcceptStates = acceptStates.flatMap { acceptState =>
      epsilonClosure(acceptState)
    }

    new NFA(
      alphabets,
      states,
      startState,
      newAcceptStates,
      newTransitions
    )
  }
}
