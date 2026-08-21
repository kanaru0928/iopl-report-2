package lr0pg
import lr0pg.Alphabet

case class Production[N, T](
    val lhs: NonTerminalAlphabet[N],
    val rhs: List[NonTerminalAlphabet[N] | TerminalAlphabet[T]]
) {
  override def toString: String = s"[$lhs -> ${rhs.mkString(" ")}]"
}

case class DottedProduction[N, T](
    val lhs: NonTerminalAlphabet[N],
    val rhs: List[NonTerminalAlphabet[N] | TerminalAlphabet[T]],
    val dotPosition: Int
) {
  override def toString: String = {
    val (beforeDot, afterDot) = rhs.splitAt(dotPosition)
    s"[$lhs -> ${beforeDot.mkString(" ")} . ${afterDot.mkString(" ")}]"
  }

  def isComplete: Boolean = dotPosition >= rhs.length
  def shift: DottedProduction[N, T] = {
    if (isComplete) {
      throw new IllegalStateException("Cannot shift a complete production.")
    } else {
      DottedProduction(lhs, rhs, dotPosition + 1)
    }
  }

  def beforeDot: List[NonTerminalAlphabet[N] | TerminalAlphabet[T]] =
    rhs.take(dotPosition)
  def afterDot: List[NonTerminalAlphabet[N] | TerminalAlphabet[T]] =
    rhs.drop(dotPosition)
  def nextSymbol: Option[NonTerminalAlphabet[N] | TerminalAlphabet[T]] = {
    if (isComplete) None else Some(rhs(dotPosition))
  }
}
