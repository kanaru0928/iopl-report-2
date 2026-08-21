package lr0pg

import scala.collection.mutable.Queue

type LR0DFAStateItem = DottedProduction[?, ?] | StartState
type LR0DFAState = Set[LR0DFAStateItem]

class StartState {
  override def toString: String = "s"
}

class StartSymbol extends NonTerminalAlphabet[String]("S'")

class EndSymbol extends TerminalAlphabet[String]("$")

enum ActionType {
  case Shift, Reduce, Accept
}

case class Action(
    val actionType: ActionType,
    val state: Option[LR0DFAState] = None,
    val production: Option[Production[?, ?]] = None
) {
  override def toString: String = actionType match {
    case ActionType.Shift =>
      s"S(${state.getOrElse(throw new IllegalStateException("Shift action requires a state."))})"
    case ActionType.Reduce =>
      s"R(${production.getOrElse(throw new IllegalStateException("Reduce action requires a production."))})"
    case ActionType.Accept => "A"
  }
}

class LR0(
    val nonTerminals: List[NonTerminalAlphabet[?]],
    val terminals: List[TerminalAlphabet[?]],
    val productions: List[Production[?, ?]],
    val startSymbol: NonTerminalAlphabet[?]
) {
  lazy val automata = DFA.fromNFA(mkFA)

  /** 表の列や遷移の走査に使う、終端記号と非終端記号の並びです。 */
  lazy val symbols: List[NonTerminalAlphabet[?] | TerminalAlphabet[?]] =
    terminals ++ nonTerminals

  override def toString: String = {
    val nonTerminalStr = nonTerminals.mkString(", ")
    val terminalStr = terminals.mkString(", ")
    val productionStr = productions.mkString("\n")
    s"LR0(\n  Non-Terminals: $nonTerminalStr,\n  Terminals: $terminalStr,\n  Productions:\n$productionStr,\n  Start Symbol: $startSymbol\n)"
  }

  private def dottedProductionTransitions(
      dottedProduction: DottedProduction[?, ?]
  ): Map[
    (
        DottedProduction[?, ?] | StartState,
        Option[NonTerminalAlphabet[?] | TerminalAlphabet[?]]
    ),
    LR0DFAState
  ] = {
    if (dottedProduction.isComplete) {
      Map.empty
    } else {
      val nextSymbol: NonTerminalAlphabet[?] | TerminalAlphabet[?] =
        dottedProduction.rhs(dottedProduction.dotPosition)
      val nextDottedProduction: DottedProduction[?, ?] | StartState =
        dottedProduction.shift

      val reductions: Map[
        (
            DottedProduction[?, ?] | StartState,
            Option[NonTerminalAlphabet[?] | TerminalAlphabet[?]]
        ),
        LR0DFAState
      ] = nextSymbol match {
        case _: NonTerminalAlphabet[?] =>
          val reduction: LR0DFAState = productions
            .filter(_.lhs == nextSymbol)
            .map { production =>
              DottedProduction(production.lhs, production.rhs, 0)
            }
            .toSet
          Map((dottedProduction, None) -> reduction)
        case _: TerminalAlphabet[?] =>
          Map.empty
      }

      Map(
        (dottedProduction, Some(nextSymbol)) -> Set(nextDottedProduction)
      ) ++ reductions
    }
  }

  def normalize: LR0 = {
    val startAlphabet = new StartSymbol
    val newNonTerminals = startAlphabet :: nonTerminals

    val endAlphabet = new EndSymbol
    val newTerminals = endAlphabet :: terminals

    val newProductions = Production(
      startAlphabet,
      List(startSymbol, endAlphabet)
    ) :: productions

    new LR0(newNonTerminals, newTerminals, newProductions, startAlphabet)
  }

  private def mkFA: NFA[
    NonTerminalAlphabet[?] | TerminalAlphabet[?],
    DottedProduction[?, ?] | StartState
  ] = {
    val alphabets: Set[NonTerminalAlphabet[?] | TerminalAlphabet[?]] =
      terminals.toSet ++ nonTerminals.toSet
    val startState = new StartState
    val states = startState :: productions.flatMap(p =>
      0.to(p.rhs.length).map(i => DottedProduction(p.lhs, p.rhs, i))
    )
    val acceptStates: LR0DFAState =
      productions
        .map(p =>
          DottedProduction(
            p.lhs,
            p.rhs,
            p.rhs.length
          )
        )
        .toSet
    val transitions: Map[
      (
          DottedProduction[?, ?] | StartState,
          Option[NonTerminalAlphabet[?] | TerminalAlphabet[?]]
      ),
      LR0DFAState
    ] = states.flatMap {
      case state: DottedProduction[?, ?] =>
        dottedProductionTransitions(state)
      case _: StartState =>
        val startProduction = productions.find(_.lhs == startSymbol).get
        val initialDottedProduction: DottedProduction[?, ?] | StartState =
          DottedProduction(startProduction.lhs, startProduction.rhs, 0)
        Some((startState, None) -> Set(initialDottedProduction))
    }.toMap

    new NFA(
      alphabets,
      states.toSet,
      startState,
      acceptStates,
      transitions
    )
  }

  lazy val actionTable: Map[
    (
        LR0DFAState,
        NonTerminalAlphabet[?] | TerminalAlphabet[?]
    ),
    Action
  ] = {
    automata.states.flatMap { state =>
      val endSymbol = EndSymbol()

      if (
        state.exists {
          case dp: DottedProduction[?, ?] =>
            dp.lhs == startSymbol && dp.rhs.last
              .isInstanceOf[EndSymbol] && dp.isComplete
          case _ => false
        }
      ) {
        Some((state, endSymbol) -> Action(ActionType.Accept))
      } else {
        val shifts = symbols.flatMap { symbol =>
          automata
            .transition(state, symbol)
            .map(nextState =>
              (state, symbol) -> Action(
                ActionType.Shift,
                state = Some(nextState)
              )
            )
        }
        val reduces = state.toSeq.collect {
          case dp: DottedProduction[?, ?] if dp.isComplete =>
            val production =
              productions.find(p => p.lhs == dp.lhs && p.rhs == dp.rhs).get
            // LR(0) では先読みに関わらず reduce するため、全終端記号の列に置きます。
            terminals.map { terminal =>
              (state, terminal) -> Action(
                ActionType.Reduce,
                production = Some(production)
              )
            }
        }.flatten
        shifts ++ reduces
      }
    }.toMap
  }

  private val invalidStates = automata.states.filter { state =>
    val (completed, incompleted) = state
      .collect { case dp: DottedProduction[?, ?] =>
        dp
      }
      .partition(_.isComplete)
    completed.size > 1 || (completed.nonEmpty && incompleted.nonEmpty)
  }

  if (invalidStates.nonEmpty) {
    throw new IllegalArgumentException(
      s"Invalid LR(0) grammar: conflicting states found: $invalidStates"
    )
  }
}

object LR0 {
  def format(lr0: LR0): String = {
    val symbols = lr0.symbols
    val numbers = stateNumbers(lr0)
    val states = numbers.toList.sortBy(_._2).map(_._1)

    val header = "State" :: symbols.map(_.toString)
    val rows = states.map { state =>
      stateName(numbers, state) :: symbols.map { symbol =>
        lr0.actionTable.get((state, symbol)).map(cell(numbers, _)).getOrElse("")
      }
    }

    val widths = (header :: rows).transpose.map(_.map(_.length).max)
    def line(row: List[String]): String =
      row.zip(widths).map { case (c, w) => c.padTo(w, ' ') }.mkString(" | ")
    val separator = widths.map("-" * _).mkString("-+-")

    val legend = states.map { state =>
      val items = state.map(_.toString).toList.sorted.mkString(", ")
      s"${stateName(numbers, state)} = { $items }"
    }

    (line(header) :: separator :: rows.map(line) ::: "" :: legend)
      .mkString("\n")
  }

  private def stateNumbers(lr0: LR0): Map[LR0DFAState, Int] = {
    val queue = Queue(lr0.automata.startState)
    var numbers = Map(lr0.automata.startState -> 0)
    while (queue.nonEmpty) {
      val state = queue.dequeue()
      for {
        symbol <- lr0.symbols
        nextState <- lr0.automata.transition(state, symbol)
        if !numbers.contains(nextState)
      } {
        numbers += nextState -> numbers.size
        queue.enqueue(nextState)
      }
    }
    numbers
  }

  private def stateName(
      numbers: Map[LR0DFAState, Int],
      state: LR0DFAState
  ): String =
    s"I${numbers(state)}"

  private def cell(numbers: Map[LR0DFAState, Int], action: Action): String =
    action.actionType match {
      case ActionType.Shift  => s"s${numbers(action.state.get)}"
      case ActionType.Reduce => s"r ${action.production.get}"
      case ActionType.Accept => "acc"
    }
}
