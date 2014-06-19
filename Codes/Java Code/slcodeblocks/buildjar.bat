@echo on

cd ../codeblocks
call buildjar.bat

cd ../slcodeblocks

mkdir build
mkdir build\support
mkdir build\support\images
del build\support\lang_def*
del build\support\*.dtd
del build\support\images\*.png

copy ..\codeblocks\build\codeblocks.jar build
copy ..\codeblocks\support\*.dtd build\support
copy support\ build\support
copy support\images build\support\images

call javac-slcodeblocks

cd build

jar cmf ..\Manifest.mf slcodeblocks.jar slcodeblocks importer breedcontroller runtimecontroller

del /q /s slcodeblocks > nul
rmdir slcodeblocks importer breedcontroller runtimecontroller

cd ..

:end
