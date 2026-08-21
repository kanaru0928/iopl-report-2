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

  val epsilonNfa = lr0Normalized.mkFA
  println(epsilonNfa)

  val nfa = epsilonNfa.purgeEpsilonTransitions
  println(nfa)

  val dfa = DFA.fromNFA(nfa)
  println(dfa)

  val outputDir = os.pwd / "output"
  os.makeDir.all(outputDir)
  os.write.over(outputDir / "epsilon-nfa.dot", epsilonNfa.toDot)
  os.write.over(outputDir / "nfa.dot", nfa.toDot)
  os.write.over(outputDir / "dfa.dot", dfa.toDot)
}
