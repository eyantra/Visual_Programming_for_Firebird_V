#!/bin/sh

(cd ../codeblocks; ./Linuxcomp.sh)
cd ../slcodeblocks

echo PREPARING FOR SLCODEBLOCKS COMPILATION
mkdir build
mkdir build/support
rm build/support/lang_def*
rm build/support/*.dtd

cp ../codeblocks/build/codeblocks.jar build/.
cp ../codeblocks/support/*.dtd build/support/.
cp support/lang_def* build/support/.

echo COMPILING SLCODEBLOCKS - Java
javac -g -deprecation -source 1.5 -target 1.5 -d build -classpath .:build/codeblocks.jar src/slcodeblocks/*.java

cd build

echo CREATING SLCODEBLOCKS.JAR
jar cmf ../Manifest.mf slcodeblocks.jar slcodeblocks

echo CLEANING UP
rm -rf slcodeblocks

cd ..
echo DONE