## Deploy Debian Package

Das Deployment/Package wird automatisch aus dem Changelog unter debian/changelog erstellt.
Um ein neues packet zu erstellen muss eine neue Version im Changelog erzeugt
werden. Das geht per Hand oder mit:

```
 dch -v X.X.X-1  testmessage
```

Im changelog bitte alle neuerungen Eintragen!

Das packet wird dann wiefolgt erstellt:

#### Changelog update
```
./release.sh update
```

Changelog prüfen und speichern! changelog.gz wird erstellt

#### Git tag erstellen
```
./release.sh tag
```
Dieser befehl macht einen commit und erzeugt den passenden Git tag.

#### Push

jetzt kann manuell nochmal alles geprüft werden. Wie in der Pipeline kann mit 
folgende schritten das paket zum test gebaut werden. 
```
apt install -y openjdk-8-jdk git
apt install curl
./gradlew wrapper --gradle-version=6.0
./gradlew build
./gradlew buildEdge
./gradlew buildEdgeDeb
```

das fertige paket kann mit rechtsklick archive manager gecheckt werden. 
Wenn alles passt kann man pushen. Der zuvor erstellte gittag wird den 
Build in der Pipeline und den upload des packages auslösen.

```
./release.sh push
```



### Text von Felix:
ReleaseTag: 
"ProjectName/release/0.0.1-4"
What is needed:
openems.jar nur noch deployen -> nicht openems-edge.jar, keinen service einrichten,
nur jar an Pfad X kopieren
Changelog bitte adden dass wir changelog einfuegen können
how to add Debian Repo and install instruction (sudo apt install openems-ProjectName)