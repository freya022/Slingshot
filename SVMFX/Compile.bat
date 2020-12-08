@echo off

set sourceFolder=../target/client/x86_64-windows
set executableName=Slingshot.exe
set inputExe="%sourceFolder%/%executableName%"
set iconPath=..\Icon.ico

if not exist ..\pom.xml (
	echo "Not a valid Maven project"
	pause
)

if not exist %sourceFolder% (
	echo "Invalid source folder path : %sourceFolder%"
	pause
)

if not exist %iconPath% (
	echo "Invalid icon path : %iconPath%"
	pause
)

cl > nul 2> nul
if %errorlevel% == 9009 (
	call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
)

set /p continue=Are you sure you want to compile %executableName% ? (y/n) : 

if "%continue%" == "y" (
	cd ..
	mvn client:build
	cd SVMFX

	start /WAIT ResourceHacker.exe -open "Test.rc" -save "Test.res" -action compile
	start /WAIT ResourceHacker.exe -open %inputExe% -save %inputExe% -action addoverwrite -res "Test.res"
	start /WAIT ResourceHacker.exe -open %inputExe% -save %inputExe% -action addoverwrite -res "%iconPath%" -mask ICONGROUP,MAINICON
)