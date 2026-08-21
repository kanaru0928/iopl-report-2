package lr0pg

class StartState {
  override def toString: String = "s"
}

class StartSymbol extends NonTerminalAlphabet[String]("S'")

class EndSymbol extends TerminalAlphabet[String]("$")

class LR0(
    val nonTerminals: List[NonTerminalAlphabet[?]],
    val terminals: List[TerminalAlphabet[?]],
    val productions: List[Production[?, ?]],
    val startSymbol: NonTerminalAlphabet[?]
) {
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
    Set[DottedProduction[?, ?] | StartState]
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
        Set[DottedProduction[?, ?] | StartState]
      ] = nextSymbol match {
        case _: NonTerminalAlphabet[?] =>
          val reduction: Set[DottedProduction[?, ?] | StartState] = productions
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
    val uniqueStartSymbol = new StartSymbol
    val startAlphabet = NonTerminalAlphabet(uniqueStartSymbol)
    val newNonTerminals = startAlphabet :: nonTerminals

    val uniqueEndSymbol = new EndSymbol
    val endAlphabet = TerminalAlphabet(uniqueEndSymbol)
    val newTerminals = endAlphabet :: terminals

    val newProductions = Production(
      startAlphabet,
      List(startSymbol, endAlphabet)
    ) :: productions

    new LR0(newNonTerminals, newTerminals, newProductions, startAlphabet)
  }

  def mkFA: NFA[
    NonTerminalAlphabet[?] | TerminalAlphabet[?],
    DottedProduction[?, ?] | StartState
  ] = {
    val alphabets: Set[NonTerminalAlphabet[?] | TerminalAlphabet[?]] =
      terminals.toSet ++ nonTerminals.toSet
    val startState = new StartState
    val states = startState :: productions.flatMap(p =>
      0.to(p.rhs.length).map(i => DottedProduction(p.lhs, p.rhs, i))
    )
    val acceptStates: Set[DottedProduction[?, ?] | StartState] =
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
      Set[DottedProduction[?, ?] | StartState]
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
}
