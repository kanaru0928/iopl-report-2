package lr0pg

@main def main(): Unit = {
  val S = NonTerminalAlphabet("S")
  val A = NonTerminalAlphabet("A")
  val open = TerminalAlphabet("(")
  val close = TerminalAlphabet(")")

  val productions = List(
    Production(S, List(A, A)),
    Production(A, List(open, A, close)),
    Production(A, List())
  )

  val lr0 = LR0(
    nonTerminals = List(S, A),
    terminals = List(open, close),
    productions = productions,
    startSymbol = S
  )

  val lr0Normalized = lr0.normalize
  println(lr0Normalized)

  val nfa = lr0Normalized.mkFA
  println(nfa)

  val dfa = DFA.fromNFA(nfa)
  println(dfa)
}
