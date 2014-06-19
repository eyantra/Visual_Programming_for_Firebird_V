#!/bin/bash

if [ "$(uname)" == "Linux" ]
then
runjavac="javac"
runjar="jar"
else
runjavac="/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Commands/javac"
runjar="/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Commands/jar"
fi

(cd ../codeblocks; ./comp.sh)
cd ../slcodeblocks

echo PREPARING FOR SLCODEBLOCKS COMPILATION
mkdir build
mkdir build/support
mkdir build/support/images
rm build/support/lang_def*
rm build/support/*.dtd

cp ../codeblocks/build/codeblocks.jar build/.
cp ../codeblocks/support/*.dtd build/support/.
cp support/* build/support/.
cp support/images/* build/support/images/.

echo COMPILING SLCODEBLOCKS - Java
$runjavac -g -deprecation -source 1.5 -target 1.5 -d build -classpath .:build/codeblocks.jar:lib/TableLayout.jar:lib/jfreechart-1.0.0-rc1.jar:lib/jcommon-1.0.0-rc1.jar src/slcodeblocks/*.java src/importer/*.java src/breedcontroller/*.java src/runtimecontroller/*.java

cd build

echo CREATING SLCODEBLOCKS.JAR
$runjar cmf ../Manifest.mf slcodeblocks.jar slcodeblocks importer breedcontroller runtimecontroller

echo CLEANING UP
rm -rf slcodeblocks importer breedcontroller runtimecontroller

cd ..
echo DONE
