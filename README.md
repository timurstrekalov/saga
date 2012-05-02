![Screenshot of example report](http://i40.tinypic.com/1s1c20.jpg)

What? Another coverage tool?
============================

Indeed. I've been using [JSCoverage](http://siliconforks.com/jscoverage/) a lot - and it proved to be a pain to use in
a Continuous Integration environment. In desperation, I wrote the
[jscoverage-maven-plugin](https://github.com/timurstrekalov/jscoverage-maven-plugin), a
[Maven](http://maven.apache.org/) wrapper around JSCoverage, which did the job, except for some huge drawbacks:

1. It directly depended on JSCoverage.
2. It only generated the statements/executed/coverage report, even though JSCoverage itself made it possible to see
    a line-by-line coverage report. Of course, I simply used jscoverage-server when I needed to see the lines I missed
    when writing tests, but, again, it's a hassle.
3. The user had to provide the path to the jscoverage binary, which most people would frown upon -
    it's just too much of a hassle. Of course, I could ship the binaries for all platforms together with the plugin
    and decide which one to use during runtime, but this just sounds sketchy every time I think about it...
4. It wasn't deployed to any Maven repository, so everyone would have to build it themselves.

Having JSCoverage as a dependency proved to be painful as well:

1. It's native - that alone makes it a pain when it comes to CI.
2. JSCoverage, as most (all?) tools only provide the total coverage report, whereas I often find myself wishing I could
    also get a coverage report for every test run.
3. It doesn't seem to be under any active development

The good things about Saga
==========================

1. It's Java. Wait, scratch that, that's not necessarily a good thing. However, it's written in Java for a reason: the
    idea was that it should be extremely simple to integrate it with Maven and any other build tool. Another reason is
    the fact that it heavily relies on [HtmlUnit](http://htmlunit.sourceforge.net/) and its internals for running
    tests and instrumentation.
2. It can generate both total coverage and per-test reports. The good thing about it is that you get more accurate
    results - unit tests are designed to cover specific units. If your total coverage report shows you high coverage
    for a unit, that does not necessarily mean that you've covered the unit that much. Actually, it doesn't mean you've
    covered it at all - it just means that some test might have invoked some parts of that unit as a side effect.
3. It has a pretty coverage report, especially after implementing the suggestions of
    [Marat Dyatko](https://github.com/vectart) :)
4. It tries to make the most of your processor power by running tests and generating coverage in parallel by default.
5. It can generate coverage even for inline scripts.
6. It's already in [Maven Central](http://repo1.maven.org/) and constantly synced, so adding it to your build cycle
    is pretty straightforward.
7. It just works. There's very little configuration, and I'd like to think that the defaults are sensible enough.
8. It's being actively developed, it's open, it's completely free.

Maven plugin usage
==================

Add the following piece of code:

    <plugin>
        <groupId>com.github.timurstrekalov</groupId>
        <artifactId>saga-maven-plugin</artifactId>
        <version>1.0.6</version>
        <executions>
            <execution>
                <phase>verify</phase>
                <goals>
                    <goal>coverage</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <baseDir>${testsBaseDir}</baseDir>
            <includes>
                **/set1/*-TestRunner.html,
                **/set2/*-TestRunner.html
            </includes>
            <outputDir>${project.build.directory}/coverage</outputDir>
        </configuration>
    </plugin>

And that's it. Wait, you also have to do

    mvn verify

Of course, there are some more configuration options, if you feel like it:

<table>
    <thead>
        <tr>
            <th>Parameter name</th>
            <th>Description</th>
            <th>Default value</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><strong>baseDir</strong></td>
            <td>The base directory for the test search</td>
            <td>none</td>
        </tr>
        <tr>
            <td><strong>includes</strong></td>
            <td>
                A comma-separated list of
                <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a> to include in
                the search for test runners
            </td>
            <td>none</td>
        </tr>
        <tr>
            <td>excludes</td>
            <td>
                A comma-separated list of
                <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a> to exclude from
                the search for test runners
            </td>
            <td>none</td>
        </tr>
        <tr>
            <td><strong>outputDir</strong></td>
            <td>The output directory for coverage reports</td>
            <td>none</td>
        </tr>
        <tr>
            <td>outputInstrumentedFiles</td>
            <td>Whether to output instrumented files. Will be written to ${outputDir}/instrumented</td>
            <td>false</td>
        </tr>
        <tr>
            <td>noInstrumentPatterns</td>
            <td>A list of regular expressions to match source file paths to be excluded from instrumentation</td>
            <td>none</td>
        </tr>
        <tr>
            <td>cacheInstrumentedCode</td>
            <td>
                Whether to cache instrumented source code. It's entirely possible that two tests might load some of the
                same resources - this would prevent them from being instrumented every time, but rather cache them for
                the whole coverage run.
            </td>
            <td>true</td>
        </tr>
        <tr>
            <td>outputStrategy</td>
            <td>One of TOTAL, PER_TEST or BOTH. Pretty self-explanatory.</td>
            <td>TOTAL</td>
        </tr>
        <tr>
            <td>threadCount</td>
            <td>The maximum number of threads to use.</td>
            <td>Runtime.getRuntime().availableProcessors()</td>
        </tr>
    </tbody>
</table>

Tested on Maven version 2.2.1 & 3.x.x

Using no-instrument patterns
----------------------------

It might be a little confusing, but the no-instrument patterns are actually [regular expressions](http://www.regular-expressions.info/) which *full script paths* are matched against. 
So, if you had a test which loaded a script like this

    <script src="someScript.js"></script>
    
If you wanted to exclude "someScript.js" from instrumentation, you would have to specify a pattern as follows:

    <!-- ... -->
    
    <noInstrumentPatterns>
        <pattern>.+/someScript\.js</pattern>
    </noInstrumentPatterns>
    
    <!-- ... -->
    
That is, if you don't care about which file named "someScript.js" gets excluded. If you do (say, you have multiple files named "someScript.js"), you would have to provide more specific patterns, such as

    <!-- ... -->
    
    <noInstrumentPatterns>
        <pattern>.+/onlyExcludeThisOne/someScript\.js</pattern>
    </noInstrumentPatterns>
    
    <!-- ... -->
    
And so forth.

Other build tools
-----------------

Using some other build tool? [Raise a ticket](https://github.com/timurstrekalov/saga/issues/new)!

Command-line tool
=================

There's also a command-line utility for those not using Maven. It has pretty much the same configuration options and works just as well.

    usage: java -jar saga-cli-<version>-jar-with-dependencies.jar -b <arg> [-e <arg>] [-f] [-h] -i <arg> [-n
       <arg>] -o <arg> [-s <arg>] [-t <arg>]
    
     -b,--base-dir <arg>                Base directory for test search
     -e,--exclude <arg>                 Comma-separated list of Ant-style
                                        paths to the tests to exclude from run
     -f,--output-instrumented-files     Whether to output instrumented files
                                        (default is false)
     -h,--help                          Print this message
     -i,--include <arg>                 Comma-separated list of Ant-style
                                        paths to the tests to run
     -n,--no-instrument-pattern <arg>   Regular expression patterns to match
                                        classes to exclude from
                                        instrumentation
     -o,--output-dir <arg>              The output directory for coverage
                                        reports
     -s,--output-strategy <arg>         Coverage report output strategy. One
                                        of [PER_TEST, TOTAL, BOTH]
     -t,--thread-count <arg>            The maximum number of threads to use
                                        (defaults to the number of cores)
                                        
A pre-built package containing the executable jar with dependencies can be downloaded from [here](http://code.google.com/p/saga-javascript-coverage/downloads/list).

Why the name?
=============

Why not? Actually, it was suggested by my girlfriend after she started reading a book on Viking history. Plus,
[Maven Central search](http://search.maven.org) only finds this project's artifacts, so there's no ambiguity.

Why doesn't my Internet Explorer show the report as nicely as it is on the screenshot above?
============================================================================================

If you're looking at a JS code coverage report with IE, then I'll [put it gently](http://www.stopusingie.com/) and
not start calling you names, even though I should. In short, Saga will never support IE-compatible reports. No graceful
degradation, no nothing. Reports may work or they may not - if they do, it's purely coincidental and I cannot be held
responsible for it.
