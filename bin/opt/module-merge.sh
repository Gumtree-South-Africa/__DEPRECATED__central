export IFS=$'\n'

# Aids in identifying duplicate files between tenant modules which are based off of each other,
# e.g. the messagecenter/*-messagecenter. Can be used to do intermediate consolidation of modules
# into e.g. a 'core-' module prior to doing a complete consolidation.

function hash() {
	md5 $1 | cut -d'=' -f2 | sed 's/\ //g'
}

function simplify() {
	grep -v '^package\ \|^import\ ' $1 | \
	  tr -s '\n' | \
	  sed -e 's/^[ \t]*//' > $2
}

BASE="ebayk-messagecenter"

# Minimize only if explicitly told to do so

[ "$1" = "y" ] && MINIMIZE=1

for i in `find ebayk-messagecenter -type f | cut -d'/' -f2- | grep -v '^target'` ; do
	echo -e "$i `hash $BASE/$i`"
done > /tmp/foo

for i in `cat /tmp/foo` ; do
	FN=$(echo $i | cut -d' ' -f1)
	HS=$(echo $i | cut -d' ' -f2)

	# Simplify and recalculate if it's a .java file

	if [[ "$FN" == *.java ]] ; then
		simplify $BASE/$FN /tmp/foo.java

		HS=`hash /tmp/foo.java`
	fi

	MATCHES=1

	for j in `ls -1 | grep -v '^mp\|^core\|robot'` ; do
		if [[ ! -f $j/$FN ]] ; then
			echo "Could not find $i in $j"

			MATCHES=0

			break
		fi

		if [[ "$FN" == *.java ]] ; then
			simplify $j/$FN /tmp/foo.java

			NAME=/tmp/foo.java
		else
			NAME=$j/$FN
		fi

		if [ ! "`hash $NAME`" = "$HS" ] ; then
			echo "Found $FN in $j but it differs"

			MATCHES=0

			break
		fi
	done

	if [ $MATCHES -eq 1 ] && [ $MINIMIZE -eq 1 ] ; then
		if [ ! -d core-messagecenter/`dirname $i` ] ; then
			mkdir -p core-messagecenter/`dirname $i`
		fi

		cp ebayk-messagecenter/$FN core-messagecenter/$FN

		for j in `ls -1 | grep -v '^core\|robot'` ; do
			[ -f $j/$FN ] && rm $j/$FN

			[ -d $j/`dirname $FN` ] &&
			  find $j/`dirname $FN` -mindepth 1 -print -quit | grep -q . || \
			    rmdir $j/`dirname $FN`
		done

		echo "File $FN occurs (pretty much) equally in all messagecenters"
	fi
done
