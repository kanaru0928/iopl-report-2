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

  val parser = Parser(lr0)

  val input = List(open, open, close, close, open, close)
  println(s"Parsing input: ${input.mkString(" ")}")
  val result = parser.parse(input)
  println(s"Parsing result: $result")
}
