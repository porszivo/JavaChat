Assignment aus 184.167 Verteilte Systeme 

Lab1 wurde in Einzelarbeit durchgeführt, Lab2 im Team.
Es handelt sich beim Chat um einen klassischen Java Commandline Chat mit einem Server und n-Clients.
Mehrere Funktionen wurden im Rahmen der Assignments implementiert

Funktionen:
## Lab1
!login <username> <password>
!logout 
!send <message>           # Sendet eine Nachricht an alle Clients (Public Message)
!register <ip:port>       # Registriert den User zum Erhalten privater Nachrichten
!lookup <username>        # Sucht den bereits registrierten User und liefert die registrierte IP zurück
!msg <receiver> <message> # Verschickt eine private Nachricht
!list                     # Zeigt alle online User an
!lastmsg                  # Gibt die letzte Nachricht zurück

## Lab2
Nameserver: User registrieren sich jetzt nicht mehr global an einem Server sondern auf ihrem zugehören Nameserver
Die Nameserver sind dabei als Baumstruktur vernetzt. Zusätzlich wird absofort mit Remoteobjekts gearbeitet.

Authentifizierung: Login wird abgelöst und es erfolgt jetzt, basieren auf dem Schlüssel des Users, eine Authentifizierung am Chatserver

Message Integrity: Diese wird absofort mit einem Hash, der durch einen SHA256 HMAC verschlüssel wird, gewährleistet.
