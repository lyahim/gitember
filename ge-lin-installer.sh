#!/bin/sh

jpackage --input shade/  --name Gitember2 --vendor "Igor Azarny"  --main-jar gitember.jar  --main-class com.az.gitember.GitemberLauncher   --type "deb"  --icon src/main/resources/icon/gitember.png

mv gitember2_1.0-1_amd64.deb Gitember2.deb


