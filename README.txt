-[DEUTSCH]-
ALGEMEIN
Multi-Thread Logik-Simulator
Ein recht umfangreicher Simulator für logische Schaltungen.
Optimiert für möglichst schnelle Simulationen durch Multithreadding.
Längere Reaktionszeiten beim bearbeiten von komplexen/großen Schaltungen aufgrund der nötigen Neukompillierung der Schaltung bei änderungen aber je nach CPU sehr hohe Simulationsgeschwindigkeiten auch bei komplexen Schaltungen.
Eine Baum-Struktur Ansicht der Simulierten Schaltung und ihrer Unterschaltungen ist verfügbar und die Unterschaltungen könnena auch während der Simulation beobachtet und bearbeitet werden.

Projekt unfertig, Gurundfunktionen bereits implementiert aber viele weitere Features sind geplant, größtenteils zur Simulation einfacher CPUs:
- Komponenten zur Interaktion mit Dateien (Einlesen von Binär-Werten aus Dateien)
- Komponenten zur graphischen Darstellung
- Komponenten zur Erfassung von Tastatur- und Maus-Eingaben
- Komponenten zur Ausgabe von Audio-Signalen
- Automatische generierung einer Warheitstabelle

Die Simulationsgeschwindigkeit kann begrenzt werden um die CPU nicht unnötig zu belasten.
Schnelle Simulationen können sehr viel Leistung ziehen und auch das Betriebssystem beeinträchtigen.
Ein Graph zur überwachung von TPS und CPU-Auslastung sind integriert.

KOMPILLIEREN
Um von dem Quellcode zu kompillieren im Unterordner LogikSimulator den gradle-task 'assembleBin' ausführen.
Die kompillierten Dateien werden automatisch in den Unterordner Runtime kopiert und bilden dort ein vollständiges Program mit allen dazugehörigen Dateien.

INSTALLATION
Eine Installation ist nicht zwingend notwendig, das Programm läuft in jedem Ordner solange Schreibzugriff auf die Konfig-Datei sichergestellt ist.
Optionall kann das setup-Skript im Programverzeichnis mit Adminrechten ausgeführt werden um die Dateiendung .lcf in der Windows Registrie auf den Editor zu konfigurieren.
Das Setup-Skipt konfiguriert die Dateiendung auf den aktuellen Pfad an dem das Programm gespeichert ist und kann beliebig neu ausgeführt werden wenn das Programm verschoben wurde.

-[ENGLISH (Automatic Translation)]-
GENERAL
Multi-threaded logic simulator
A quite comprehensive simulator for logic circuits.
Optimized for the fastest possible simulations through multithreading.
Longer response times when processing complex/large circuits due to the necessary recompilation of the circuit when changes are made, but depending on the CPU, very high simulation speeds even with circuits that are more complex.
A tree structure view of the simulated circuit and its sub-circuits is available and the sub-circuits can also be observed and edited during the simulation.

Project unfinished, basic functions already implemented, but many more features are planned, mainly for simulating simple CPUs:
- Components for interacting with files (reading binary values ​​from files)
- Components for graphical representation
- Components for capturing keyboard and mouse input
- Components for outputting audio signals
- Automatic generation of a commodity table

The simulation speed can be limited in order not to load the CPU unnecessarily.
Fast simulations can draw a lot of power and also affect the operating system.
A graph to monitor TPS and CPU usage are integrated.

COMPILE
To compile from the source code, run the 'assembleBin' Gradle task in the LogikSimulator subfolder.
The compiled files are automatically copied to the Runtime subfolder, where they form a complete program with all required files.

INSTALLATION
An installation is not absolutely necessary, the program runs in every folder as long as write access to the config file is guaranteed.
Optionally, the setup script can be run in the program directory with admin rights in order to configure the .lcf file extension in the Windows registry on the editor.
The setup script configures the file extension to the current path where the program is saved and can be freely re-run if the program has been moved.