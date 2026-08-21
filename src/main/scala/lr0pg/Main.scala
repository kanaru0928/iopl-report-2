package lr0pg

@main def main(): Unit = {
  val S = NonTerminalAlphabet("S")
  val A = NonTerminalAlphabet("A")
  val open = TerminalAlphabet("(")
  val close = TerminalAlphabet(")")

  val productions = List(
    Production(S, List(A, A)),
    Production(A, List(open, close)),
    Production(A, List(open, A, close))
  )

  val lr0 = LR0(
    nonTerminals = List(S, A),
    terminals = List(open, close),
    productions = productions,
    startSymbol = S
  )

  val lr0Normalized = lr0.normalize
  println(lr0Normalized)

  val dfa = lr0Normalized.automata
  println(dfa)

  val outputDir = os.pwd / "output"
  os.makeDir.all(outputDir)
  os.write.over(outputDir / "dfa.dot", dfa.toDot)
}
