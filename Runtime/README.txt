-----------------------------------------------
[INSTALATION]

The Programm can be used portable in any directory.
But optionally the file extension .lcf can be configured to point to the LogikSimulator on a fixed location.
To configure it, first copy the programm into an folder that needs no admin rights to access and then run the setup-skript file WITH ADMIN RIGHTS.
After the setup skript has ben run, it should be possible to open circuit files by doubleclicking on them.
-----------------------------------------------
[CONTROLS]

Basic controlls in the Circuit-Editor:
Left-Click		Move/Select Components or Nodes
Double-Left-Click	Move/Select Components or Nodes
Right-Click		Controll Component (turn switch on/of, edit value for simulation), moving camera
Middle-Click		Moving camera
Delete-Key		Delete component under cursor
Controll+C		Copy currently selected Components
Controll+V		Place components from clipboard and automatically select them to move them around
Escape-Key		Abbort placing components
NOTE: When pasting stuff from the clipboard, the Escape-Key only abborts the selection of the pasted components, it does not remove the compontents.

Concept of the editor:
To start the simulation first the main circuit must be selected, this is done via the "Set as main" Button left from the Start Stop and Pause buttons.
After that the simulation can be started stoped and paused.
When started the first time, the output may be random since the sub-circuits have to get initialized first.
By stoping and restarting the simulation a second time the randomly set bits can all be cleared to zero.

Circuits can be edited while simulated.
When right-clicking a sub-circuit component in an simulated circuit, this components circuit is opened in an own editor window and can be observed and edited.
The simulation speed is limited by the number-field right from the CPU/TPS-graph.
A value of 0 desables the limiter, but this can bring the CPU up to 100% usage.
If the limiter is disabled, the simulation runs as fast as possible on the current hardware.

-----------------------------------------------
[COMMON ERRORS]

Due to the multi-threading, errors can appear when running a simulation while editing its circuits.
Normaly these errors are detected by the program and simply ignored (most of them occure while rendering the components to the screen).
Sometimes critical errors in the simulation can appear, in this case a warning is displayed and all affected threads are closed.
In most cases the simulation can even continue in this case since other threads continue with the operations, but its better to restart the programm should this happen.
-----------------------------------------------
