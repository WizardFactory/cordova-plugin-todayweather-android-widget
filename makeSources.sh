#!/bin/sh

while read foo
do
	src=${foo#*/}
	dst=${src#*/}
	dst=${dst#*/}
	dst=${dst%/*}
	echo "<source-file src=\"$src\" target-dir=\"$dst\" />"
done
