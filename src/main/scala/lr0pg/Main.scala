package lr0pg

@main def main(): Unit = {
  val nfa = new NFA(
    Set(0, 1),
    Set(0, 1, 2),
    0,
    Set(2),
    Map(
      (0, Some(0)) -> Set(0, 1),
      (0, Some(1)) -> Set(0),
      (1, Some(1)) -> Set(2)
    )
  )
  println(nfa)

  val dfa = DFA.fromNFA(nfa)
  println(dfa)
}
