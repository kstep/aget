all: install start

bin/MainActivity-debug.apk: src/me/kstep/aget/*.java

compile: bin/MainActivity-debug.apk
	ant debug

install: compile
	ant installd

start:
	adb shell am start -n me.kstep.aget/.MainActivity_

.PHONY: compile install all start
