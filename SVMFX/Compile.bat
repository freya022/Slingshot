@echo off

if not exist ..\pom.xml (
	echo "Not a valid Maven project"
	pause
)

cl > nul 2> nul
if %errorlevel% == 9009 (
	call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
)

cd ..
call mvn gluonfx:compile
cd SVMFX

@REM start /WAIT rc.exe Resources.rc
@REM start /WAIT cvtres.exe Resources.res

cd ..
call mvn gluonfx:link
cd SVMFX