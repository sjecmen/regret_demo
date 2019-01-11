Developing
==========

Dependencies
------------

Below is a list of all of the dependencies followed by their name in standard Ubuntu repositories.

1. Java 1.8 (`openjdk-8-jdk`)
2. Make (`make`)
3. Maven 3 (`maven`)
4. Jq (`jq`)
5. Git (`git`)

Compiling & Common Tasks
------------------------

### Make

There is a Makefile at the root of market-sim that provides targets for mos operations you would want to accomplish.
Typing `make help` will display information about all of them.

<dl>
  <dt>jar</dt>
  <dd>Compile java into an executable jar that can be called with `market-sim.sh`, or `run-egta.sh` This essentially updates the current executable simulator with the current version in source.</dd>

  <dt>test</dt>
  <dd>
    Run all java unit tests.
    Make sure that this passes before merging code into a larger branch.
  </dd>

  <dt>egta def=\<defaults.json\></dt>
  <dd>
    Compile the current jar into an egta compatible zip file.
    This requires that `def` be specified.
    `def` is the path to the egta `defaults.json` you wish to use for this jar.
    It will also be used for the name of the resulting zip file, so make sure that the name of the `defaults.json` you're using coincides with the desored simulator name.
  </dd>

  <dt>report</dt>
  <dd>
    Generate and display a number of reports about the java source code.
    In general this should be free of most of the errors is points out.
    This should be run before a large code change.
  </dd>

  <dt>docs</dt>
  <dd>Compile the documentation markdown files into pdfs for easier viewing.</dd>

  <dt>clean</dt>
  <dd>
    Remove all java byproducts.
    This probably won't be necessary, but it exists just in case.
  </dd>

To remove the syntax highlighting add `COLOR=cat` to any command invocation.


Importing Into Eclipse
----------------------

1. Set your eclipse workspace to the root `market-sim` directory, i.e. if this file's path is `.../market-sim/docs/developing.md`, then then your eclipse workspace should be `.../market-sim`.
2. Go to `File` > `Import...` > `Maven` > `Existing Maven Projects`.
3. Click browse and select the nested market-sim directory, e.g. `.../market-sim/marketsim`.
4. Click Finish, and the project should begin importing and be all setup.
5. The unit tests can be run inside eclipse by right clicking `src/test/java` and then `Run As` > `JUnit Test`.


Style
-----

We currently use the [Google Style Guide](http://google.github.io/styleguide/javaguide.html).
In the `resouces` folder there are two files (`eclipse-java-google-style.xml` and `google-style.importorder`) to help enforce this style in Eclipse.

### Import the Code Formatter

1. Right click on the market-sim project and go to `properties`.
2. Here go to `Java Code Style` > `Formatter`.
3. If you haven't already done so check `Enable project specific settings`.
4. Click `Import...` and find the `eclipse-java-google-style.xml` file, which should be in `market-sim/resources`.
5. The formatter should be automatically selected, simply hit `Apply`.

You should now be able to right click on any resource and go to `Source` > `Format` which will auto format your code to meet the style standards.

### Import the Import Order

1. Right click on the market-sim project and go to `properties`.
2. Here go to `Java Code Style` > `Organize Imports`.
3. If you haven't already done so check `Enable project specific settings`.
4. Click `Import...` and find the `google-style.importorder` file, which should be in `market-sim/resources`.
5. The settings should be automatically updated, simply hit `Apply`.

You should now be able to right click on any resource and go to `Source` > `Organize Imports` which will auto format  organize your import statements to meet the style standards.
