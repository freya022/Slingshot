<h1>Slingshot</h1>

<h2>A fast minecraft launcher</h2>
<p>This project is mostly made in Java but uses C++ methods in critical places like file checking and performing HTTP(S) requests</p>
<p>The launcher can also be compiled into a native executable and will save you startup time, memory usage and snappiness</p>

<h2>Why ?</h2>
Some friends and I decided to play Modded Minecraft, we used the Technic Launcher for some time, but it suffers from several weaknesses :
<ul>
    <li>Forced to have Internet access</li>
    <li><b>Very slow startup</b></li>
    <li>It's Windows version is wrapped, forces us to use java 8, disallows higher versions</li>
    <li>Using the Linux/MacOS version on Windows is fine though, but double clicking on it will prevent itself from having command line arguments like enabling anti aliasing and non-latin symbols</li>
    <li><b>Extremely slow</b> modpack upgrading / downgrading, I suspect they tried to keep the launcher single threaded to display accurately download status, combine that to slow mod hosting servers and you got a 5 minute long modpack update</li>
    <li>A JAR file cannot be pinned on the taskbar nor the Start menu on Windows (and probably Linux too), making it a chore to find the launcher on the PC</li>
</ul>

<p>These issues are fixed by using a much more modern GUI toolkit such as JavaFX 
and by using "Ahead of Time" compilation to make native executable, which are also pin-able, and using multiple threads to achieve certain tasks.</p>
<p>Taking the modpack upgrade example shows us that (approximately), Technic launcher took about 5 minutes to upgrade a modpack, while my launcher took advantage of my Internet capabilities and upgraded that same modpack in about 10 seconds</p>