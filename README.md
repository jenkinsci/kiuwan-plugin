Jenkins Kiuwan plugin
===============

Run Kiuwan static analysis of your code as part of your continuous integration process with Jenkins.
http://wiki.jenkins-ci.org/display/JENKINS/Kiuwan+Plugin


Installing
----------
You should usually just install the Bazaar plugin from your Jenkins
Management console (look under available plugins)


Building
--------

$ mvn hpi:run

This will build the plugin, grab everything needed and start you up a
fresh Jenkins instance on a TCP/IP port for you to test against.

Maven does have a habit of downloading the internet, but it's at least
easy to use to hack on a plugin of something.