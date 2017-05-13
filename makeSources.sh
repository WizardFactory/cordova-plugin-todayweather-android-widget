#!/bin/sh

while read foo
do
	src=${foo#*/}
	dst=${src#*/}
	dst=${dst#*/}
	echo "<resource-file src=\"$src\" target=\"$dst\" />"
done
