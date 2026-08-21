SOURCES = $(wildcard src/main/scala/lr0pg/*)

output/dfa.dot: $(SOURCES)
	sbt run

output/dfa.png: output/dfa.dot
	dot -Tpng output/dfa.dot -o output/dfa.png

.PHONY: png
png: output/dfa.png

.PHONY: dot
dot: output/dfa.dot
