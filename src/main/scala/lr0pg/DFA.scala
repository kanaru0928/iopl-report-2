package lr0pg

import scala.collection.mutable.Queue

class DFA[S, Q](
    val alphabets: Set[S],
    val states: Set[Q],
    val startState: Q,
    val acceptStates: Set[Q],
    transitions: Map[(Q, S), Q]
) {
  def transition(state: Q, input: S): Option[Q] =
    transitions.get((state, input))

  /** Graphviz の DOT 形式に変換する。 */
  def toDot: String = Graphviz.digraph(
    "DFA",
    states,
    startState,
    acceptStates,
    transitions.toSeq.map { case ((state, input), nextState) =>
      (state, input.toString, nextState)
    }
  )

  override def toString: String = {
    val transitionStrings = transitions.map {
      case ((state, input), nextState) =>
        s"$state --$input--> $nextState"
    }
    s"DFA(\n  States: ${states.mkString(", ")},\n  Start State: $startState,\n  Accept States: ${acceptStates.mkString(", ")},\n  Transitions:\n    ${transitionStrings.mkString("\n    ")}\n)"
  }
}

object DFA {
  def fromNFA[S, Q](nfa: NFA[S, Q]): DFA[S, Set[Q]] = {
    val startState = Set(nfa.startState)
    val alphabets = nfa.alphabets

    var transitions = Map.empty[(Set[Q], S), Set[Q]]
    var visitedStates = Set(startState)

    val queue = Queue[Set[Q]]()
    queue.enqueue(startState)
    while (queue.nonEmpty) {
      val currentStateSet = queue.dequeue()
      for (alphabet <- alphabets) {
        val nextStateSet = currentStateSet.flatMap(state =>
          nfa.transition(state, Some(alphabet))
        )
        if (nextStateSet.nonEmpty) {
          transitions += (currentStateSet, alphabet) -> nextStateSet
          if (!visitedStates.contains(nextStateSet)) {
            visitedStates += nextStateSet
            queue.enqueue(nextStateSet)
          }
        }
      }
    }

    val acceptStates = visitedStates.filter(stateSet =>
      stateSet.exists(state => nfa.acceptStates.contains(state))
    )

    new DFA(
      alphabets,
      visitedStates,
      startState,
      acceptStates,
      transitions
    )
  }
}
