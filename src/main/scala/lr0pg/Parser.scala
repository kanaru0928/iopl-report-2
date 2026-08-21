package lr0pg

import scala.collection.mutable.Stack
import scala.collection.mutable.Queue

class Parser(val lr0: LR0) {
  val normalizedLR0 = lr0.normalize

  def parse(input: List[TerminalAlphabet[?]]): Boolean = {
    val inputWithEndSymbol =
      Queue[TerminalAlphabet[?] | NonTerminalAlphabet[?]](
        input :+ EndSymbol(): _*
      )
    val stack = Stack[Set[DottedProduction[?, ?] | StartState]](
      normalizedLR0.automata.startState
    )
    var x: TerminalAlphabet[?] | NonTerminalAlphabet[?] =
      inputWithEndSymbol.dequeue()
    val table = normalizedLR0.actionTable

    while (true) {
      val action = table.getOrElse(
        (stack.top, x),
        throw new IllegalStateException(
          s"No action found for state ${stack.top} and symbol $x."
        )
      )

      action.actionType match {
        case ActionType.Shift =>
          val currentState = action.state.getOrElse(
            throw new IllegalStateException("Shift action requires a state.")
          )
          stack.push(currentState)
          if (inputWithEndSymbol.nonEmpty) {
            x = inputWithEndSymbol.dequeue()
          }

        case ActionType.Reduce =>
          val production = action.production.getOrElse(
            throw new IllegalStateException(
              "Reduce action requires a production."
            )
          )
          0 until production.rhs.length foreach { _ =>
            stack.pop()
          }
          inputWithEndSymbol.prepend(x)
          x = production.lhs

        case ActionType.Accept =>
          return true
      }
    }

    false
  }
}
