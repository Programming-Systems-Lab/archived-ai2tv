# Makefile for ckptmgr paper, intended to be submitted to 
# OSDI 2004

# this make file should create the pdf and ps files.

all: dvi ps pdf

.PHONY: all dvi clean ps pdf

name=ai2tv

ps: ${name}.ps
dvi: ${name}.dvi
pdf: ${name}.pdf

SRC = ${name}.tex
TEX=latex

DVIPSFLAGS=-K

%.dvi: %.tex; latex $*; latex $*; bibtex $*; latex $*;
%.ps: %.dvi; dvips $(DVIPSFLAGS) $* -o
# %.pdf: %.tex %.dvi; pdflatex $*
%.pdf: %.ps; ps2pdf $< $@

# ${name}.dvi: ${name}.sty

clean:
	-rm -f ${name}.dvi ${name}.ps ${name}.log ${name}.aux ${name}.bbl ${name}.blg
# 	-rm -f ${name}.{dvi,ps,log,aux,bbl,blg}

