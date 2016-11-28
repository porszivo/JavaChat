Reflect about your solution!

Summary:
 - Bei der Implementierung kam es hauptsächlich darauf an ein gutes Threadmanagement zu haben.
 - Der Server hat für jeden Client einen eigenen Listener der bei Anfrage vom Client einen extra Thread anlegt.
 - Die Bearbeitung der Kommandos vom Client werden dann im MessageHandler bearbeitet und mit einem Rückgabewert 
	an den Client geschickt
 - Der Client nimmt dann die Rückgaben in einem extra Thread entgegen. Dieser wird benötigt um ggf. parallel ankommende
   Rückgaben zu speichern und dann über eine Queue abgearbeitet werden /inkl. ausgegeben wird.
 - Wichtig war auch beim Schließen aller Threads die sockets, serversockets und datagrams zu schließen, außerdem die shell
   und alle Reader und Writer

TEST COMMIT