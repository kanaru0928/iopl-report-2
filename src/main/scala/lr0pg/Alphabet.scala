package lr0pg

sealed abstract class Alphabet[+T]

case class TerminalAlphabet[+T](val symbol: T) extends Alphabet[T] {
  override def toString: String = symbol.toString()
}

case class NonTerminalAlphabet[+T](val symbol: T) extends Alphabet[T] {
  override def toString: String = symbol.toString()
}
