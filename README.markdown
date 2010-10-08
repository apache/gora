Gora Project
============

For the latest information about Gora, please visit our website at:

   http://github.com/enis/gora

and the wiki page at:

   http://wiki.github.com/enis/gora/

How to check out project
------------------------

Gora uses git distributed version tracking. Source code for gora resides in 
GitHub server. To checkout the code from git use

$ git clone git://github.com/enis/gora.git

adding changes to local repository can be done like
$ touch first.txt
$ git add .
$ git commit -m "added first.txt file" 

then to send the changes(local commits to the server) use
$git push

to fetch and merge the updates to the server use
$git pull origin master


Building the project
--------------------

Gora uses ant and ivy as the build tool and dependency manager respectively. 
Install ant (on debian systems)
 $ sudo apt-get install ant 
ivy is downloaded and installed at first build. 

To build the project 

$cd gora
$ant 

which will build all the modules in the project and deploys the jar files to 
local ivy repository. To build a specific module, cd to that module and run 
ant there. 

Gora project uses modular architecture. Each module has its own directory under
top-level. The gore-core module contains most of the shared code between the modules. 

Dependency management is handled by ivy. Required libraries are specified at 
ivy.xml files. Default configuration uses the official maven repository. 
Libraries not found on the maven repository is distributed along the module 
under lib-ext dir.

Developing with Eclipse
-----------------------

Gora is a modular project, so every module has to be imported to Eclipse 
separately. First start with gora-core, File -> New -> Project -> Java Project 
from Existing Ant Buildfile -> select gora-core/build.xml. Repeat for every 
module you wish to develop. Also add dependency to the gora-core project. 

Gora development 
----------------

Gora is currently developed by a small team. The contributers of Gora 
can be found at http://wiki.github.com/enis/gora/who-we-are

However, we encourage all kinds of help 
from the community. More information can be found at
http://wiki.github.com/enis/gora/how-to-contribute

Mailing list
------------
Gora mailing list is hosted at google groups. Please direct any questions, feature requests, 
and project releted announcements to the mailing list.

Current web address:
http://groups.google.com/group/gora-dev

Current email address:
gora-dev@googlegroups.com

License
-------

Gora is provided under Apache License version 2.0. See LICENSE.txt for more details.

