all: install start

bin/aGet-debug.apk: src/me/kstep/aget/*.java

compile: bin/aGet-debug.apk
	ant debug

install: compile
	ant installd

start:
	adb shell am start -n me.kstep.aget/.DownloadManagerActivity_

.PHONY: compile install all start
