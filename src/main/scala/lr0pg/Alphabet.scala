package lr0pg

sealed abstract class Alphabet[T]

case class TerminalAlphabet[T](val symbol: String) extends Alphabet[T] {
  override def toString: String = symbol
}

case class NonTerminalAlphabet[T](val symbol: String) extends Alphabet[T] {
  override def toString: String = symbol
}
