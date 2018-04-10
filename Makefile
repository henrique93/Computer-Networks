JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	User.java \
	TCS.java \
	TRS.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class languages.txt
