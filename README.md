# Dart4E - Dart/Flutter support for the Eclipse IDE

[![Build Status](https://github.com/dart4e/dart4e/actions/workflows/build.yml/badge.svg)](https://github.com/dart4e/dart4e/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/dart4e/dart4e.svg?color=blue)](LICENSE.txt)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v2.0%20adopted-ff69b4.svg)](CODE_OF_CONDUCT.md)


**Feedback and high-quality pull requests are  highly welcome!**

1. [About](#about)
1. [Installation](#installation)
1. [Building from Sources](#building)
1. [Acknowledgement](#acknowledgement)
1. [License](#license)


## <a name="about"></a>About

Dart4E is an extension for the [Eclipse IDE](https://eclipse.org) to support development using the [Dart](https://dart.dev) general purpose
programming language and the [Flutter](https://flutter.dev/) application framework.

### Features
- Syntax Highlighting
- Source Code Formatting
- Auto Completion
- Code Refactoring
- [Interactive Dart Shell](https://github.com/fzyzcjy/dart_interactive) (REPL)
- Running and debugging of Dart programs/tests and Flutter applications
- Display direct and transitive dependencies in project outline

![](src/site/images/screenshot_editor.png)

![](src/site/images/screenshot_dartmenu.png)

![](src/site/images/screenshot_debugger.png)


## <a name="installation"></a>Installation

If you don't have Eclipse installed you can install [Dart4E Studio](https://github.com/dart4e/dart-studio) - a custom Eclipse distribution - which has this plugin preinstalled.

To install Dart4E into an existing Eclipse installation do:
1. Within Eclipse go to: Help -> Install New Software...
1. Enter the following update site URL: https://raw.githubusercontent.com/dart4e/dart4e/updatesite
1. Select the `Dart4E` feature to install.
1. Ensure that the option `Contact all update sites during install to find required software` is enabled.
1. Click `Next` twice.
1. Read/accept the license terms and click `Finish`.
1. Eclipse will now download the necessary files in the background.
1. When the download has finished, Eclipse will ask about installing unsigned content. You need to accept if you want to
1. After installation you will be prompted for a restart of Eclipse, which is recommended.


## <a id="building"></a>Building from Sources

To ensure reproducible builds this Maven project inherits from the [vegardit-maven-parent](https://github.com/vegardit/vegardit-maven-parent)
project which declares fixed versions and sensible default settings for all official Maven plug-ins.

The project also uses the [maven-toolchains-plugin](http://maven.apache.org/plugins/maven-toolchains-plugin/) which decouples the JDK that is
used to execute Maven and it's plug-ins from the target JDK that is used for compilation and/or unit testing. This ensures full binary
compatibility of the compiled artifacts with the runtime library of the required target JDK.

To build the project follow these steps:

1. Download and install a Java 17 SDK, e.g. from:
   - https://github.com/ojdkbuild/ojdkbuild
   - https://adoptium.net/releases.html?variant=openjdk17
   - https://www.azul.com/downloads/?version=java-17-lts&architecture=x86-64-bit&package=jdk#download-openjdk

1. Download and install the latest [Maven distribution](https://maven.apache.org/download.cgi).

1. In your user home directory create the file `.m2/toolchains.xml` with the following content:

   ```xml
   <?xml version="1.0" encoding="UTF8"?>
   <toolchains>
      <toolchain>
         <type>jdk</type>
         <provides>
            <version>17</version>
            <vendor>default</vendor>
         </provides>
         <configuration>
            <jdkHome>[PATH_TO_YOUR_JDK_17]</jdkHome>
         </configuration>
      </toolchain>
   </toolchains>
   ```

   Set the `[PATH_TO_YOUR_JDK_17]` parameter accordingly.

1. Checkout the code using one of the following methods:

    - `git clone https://github.com/dart4e/dart4e`
    - `svn co https://github.com/dart4e/dart4e dart4e`

1. Run `mvn clean verify` in the project root directory. This will execute compilation, unit-testing, integration-testing and
   packaging of all artifacts.


## <a name="acknowledgement"></a>Acknowledgement

Dart4E was created by [Sebastian Thomschke](https://github.com/sebthom) and is sponsored by [Vegard IT GmbH](https://www.vegardit.com).

Dart4E would not have been possible without the following technologies and learning resources:

**Technologies/Libraries**
- [dart_interactive](https://github.com/fzyzcjy/dart_interactive) - Interactive Shell (REPL) for Dart
- [Eclipse Platform](https://github.com/eclipse-platform)
- [Eclipse LSP4E](https://projects.eclipse.org/projects/technology.lsp4e) - Language Server Protocol for Eclipse
- [Eclipse TM4E](https://projects.eclipse.org/projects/technology.tm4e) - TextMate support for Eclipse
- [Eclipse Tycho](https://projects.eclipse.org/projects/technology.tycho) - tools to build Eclipse plug-ins with Maven
- [Eclipse RedDeer](https://projects.eclipse.org/projects/technology.reddeer) - UI testing framework

**Tutorials**
- https://eclipse.org/articles
    - [Understanding Decorators](https://www.eclipse.org/articles/Article-Decorators/decorators.html)
    - [Using Progress Monitors](http://www.eclipse.org/articles/Article-Progress-Monitors/article.html)
    - [On the Job: The Eclipse Jobs API](http://www.eclipse.org/articles/Article-Concurrency/jobs-api.html)
- https://blogs.itemis.com
    - [Eclipse Actions für Project und Package Explorer](https://blogs.itemis.com/auf-einen-blick-eclipse-actions-f%C3%BCr-project-und-package-explorer)
- https://www.vogella.com/tutorials
    - [Eclipse IDE Plug-in Development: Plug-ins, Features, Update Sites and IDE Extensions](https://www.vogella.com/tutorials/EclipsePlugin/article.html)
    - [Creating Eclipse Wizards](https://www.vogella.com/tutorials/EclipseWizards/article.html)
    - [Eclipse Project Natures](https://www.vogella.com/tutorials/EclipseProjectNatures/article.html)
    - [Developing an Eclipse language server integration](https://www.vogella.com/tutorials/EclipseLanguageServer/article.html)
    - [Defining custom launcher for the Eclipse IDE](https://www.vogella.com/tutorials/EclipseLauncherFramework/article.html)
    - [Testing Eclipse application with the RedDeer framework](https://www.vogella.com/tutorials/EclipseRedDeer/article.html)
- http://blog.eclipse-tips.com
    - [Selection Dialogs in Eclipse](http://blog.eclipse-tips.com/2008/07/selection-dialogs-in-eclipse.html)


## <a name="license"></a>License

If not otherwise specified (see below), files in this repository fall under the [Eclipse Public License 2.0](LICENSE.txt).

Individual files contain the following tag instead of the full license text:
```
SPDX-License-Identifier: EPL-2.0
```

This enables machine processing of license information based on the SPDX License Identifiers that are available here: https://spdx.org/licenses/.

An exception is made for:
1. files in readable text which contain their own license information, or
2. files in a directory containing a separate `LICENSE.txt` file, or
3. files where an accompanying file exists in the same directory with a `.LICENSE.txt` suffix added to the base-name of the original file.
   For example `foobar.js` is may be accompanied by a `foobar.LICENSE.txt` license file.
