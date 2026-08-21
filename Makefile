output/epsilon-nfa.dot output/nfa.dot output/dfa.dot: src/main/scala/lr0pg/Main.scala
	sbt run

output/dfa.png: output/dfa.dot
	dot -Tpng output/dfa.dot -o output/dfa.png

output/nfa.png: output/nfa.dot
	dot -Tpng output/nfa.dot -o output/nfa.png

output/epsilon-nfa.png: output/epsilon-nfa.dot
	dot -Tpng output/epsilon-nfa.dot -o output/epsilon-nfa.png

.PHONY: png
png: output/epsilon-nfa.png output/nfa.png output/dfa.png

.PHONY: dot
dot: output/epsilon-nfa.dot output/nfa.dot output/dfa.dot
