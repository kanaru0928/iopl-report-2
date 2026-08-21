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
    val nonEpsilonNFA = nfa.purgeEpsilonTransitions

    val startState = Set(nonEpsilonNFA.startState)
    val alphabets = nonEpsilonNFA.alphabets

    var transitions = Map.empty[(Set[Q], S), Set[Q]]
    var visitedStates = Set(startState)

    val queue = Queue[Set[Q]]()
    queue.enqueue(startState)
    while (queue.nonEmpty) {
      val currentStateSet = queue.dequeue()
      for (alphabet <- alphabets) {
        val nextStateSet = currentStateSet.flatMap(state =>
          nonEpsilonNFA.transition(state, Some(alphabet))
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
      stateSet.exists(state => nonEpsilonNFA.acceptStates.contains(state))
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
