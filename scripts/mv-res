#!/bin/bash

echo_error() {
	echo "$(tput setaf 1)$1$(tput sgr0)"
}

echo_success() {
	echo "$(tput setaf 2)$1$(tput sgr0)"
}

main() {
	if [ -z $1 ] || [ -z $2 ]; then
		echo "usage: $(basename $0) <src-dir> <dest-dir>"
		exit
	fi

	SRC_DIR=$1
	DEST_DIR=$2

	if ! [[ $DEST_DIR == */res ]]; then
		echo_error "<dest-dir> must be a projects 'res' folder."
		exit
	fi

	echo_success "> Moving xxhdpi assets"
	for f in *@3x.png; do
		SANITIZED=$(echo "$f" | sed -E 's/\@3x//g')
		mv -f "$SRC_DIR/$f" "$DEST_DIR/drawable-xxhdpi/$SANITIZED"
	done

	echo_success "> Moving xhdpi assets"
	for f in *@2x.png; do
		SANITIZED=$(echo "$f" | sed -E 's/\@2x//g')
		mv -f "$SRC_DIR/$f" "$DEST_DIR/drawable-xhdpi/$SANITIZED"
	done

	echo_success "> Moving mhdpi assets"
	for f in *.png; do
		mv -f "$SRC_DIR/$f" "$DEST_DIR/drawable-mdpi/$f"
	done
	
	exit
}

main $1 $2