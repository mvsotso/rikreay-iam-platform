#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
#
# Optional ENV vars
#   JAVA_HOME - location of a JDK home dir
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ]; then

  if [ -f /usr/local/etc/mavenrc ]; then
    . /usr/local/etc/mavenrc
  fi

  if [ -f /etc/mavenrc ]; then
    . /etc/mavenrc
  fi

  if [ -f "$HOME/.mavenrc" ]; then
    . "$HOME/.mavenrc"
  fi

fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
CYGWIN*) cygwin=true ;;
MINGW*) mingw=true ;;
Darwin*) darwin=true
  # Use /usr/libexec/java_home if available, otherwise fall back
  # to JAVA_HOME
  if [ -z "$JAVA_HOME" ]; then
    if [ -x "/usr/libexec/java_home" ]; then
      JAVA_HOME="$(/usr/libexec/java_home)"; export JAVA_HOME
    fi
  fi
  ;;
esac

if [ -z "$JAVA_HOME" ]; then
  if [ -r /etc/gentoo-release ]; then
    JAVA_HOME="$(java-config --jre-home)"
  fi
fi

# For Cygwin and MinGW, ensure paths are in UNIX format before
# temporary files are created
if $cygwin || $mingw; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  [ -n "$CLASSPATH" ] && CLASSPATH="$(cygpath --path --unix "$CLASSPATH")"
fi

# For MinGW, ensure paths are in native Windows format
if $mingw; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME="$(cygpath --windows "$JAVA_HOME")"
fi

# Resolve the project base directory
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd -P)}"
export MAVEN_PROJECTBASEDIR

MAVEN_OPTS="$(sed -n 's/^[[:space:]]*\(MAVEN_OPTS=\)\(.*\)/\2/p' "$MAVEN_PROJECTBASEDIR/.mvn/jvm.config" 2>/dev/null) ${MAVEN_OPTS}"
export MAVEN_OPTS

# Load argfiles
MAVEN_CONFIG=""
if [ -f "$MAVEN_PROJECTBASEDIR/.mvn/maven.config" ]; then
  MAVEN_CONFIG="$(cat "$MAVEN_PROJECTBASEDIR/.mvn/maven.config")"
fi

WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ -r "$WRAPPER_JAR" ]; then
  if $cygwin; then
    WRAPPER_JAR="$(cygpath --path --windows "$WRAPPER_JAR")"
    MAVEN_PROJECTBASEDIR="$(cygpath --path --windows "$MAVEN_PROJECTBASEDIR")"
  fi

  # shellcheck disable=SC2086
  exec "$JAVACMD" \
    $MAVEN_OPTS \
    $MAVEN_DEBUG_OPTS \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    ${WRAPPER_LAUNCHER} $MAVEN_CONFIG "$@"
fi

# Fallback: download wrapper jar if not present
WRAPPER_URL="$(sed -n 's/^[[:space:]]*wrapperUrl=\(.*\)/\1/p' "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties")"
DISTRIBUTION_URL="$(sed -n 's/^[[:space:]]*distributionUrl=\(.*\)/\1/p' "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties")"

if [ -z "$DISTRIBUTION_URL" ]; then
  echo "Error: distributionUrl is not set in maven-wrapper.properties" >&2
  exit 1
fi

# Determine JAVA_HOME / JAVACMD
if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ]; then
    echo "The JAVA_HOME environment variable is not defined correctly," >&2
    echo "this environment variable is needed to run this program." >&2
    exit 1
  fi
else
  JAVACMD="$(\unset -f command 2>/dev/null; \command -v java)" || :
  if [ ! -x "$JAVACMD" ]; then
    echo "The java command is not available in PATH or JAVA_HOME is not set." >&2
    exit 1
  fi
fi

# Use the maven-wrapper.jar if present, otherwise download Maven directly
if [ -r "$WRAPPER_JAR" ]; then
  if $cygwin; then
    WRAPPER_JAR="$(cygpath --path --windows "$WRAPPER_JAR")"
    MAVEN_PROJECTBASEDIR="$(cygpath --path --windows "$MAVEN_PROJECTBASEDIR")"
  fi

  exec "$JAVACMD" \
    $MAVEN_OPTS \
    $MAVEN_DEBUG_OPTS \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    ${WRAPPER_LAUNCHER} $MAVEN_CONFIG "$@"
else
  # Download Maven distribution directly (no wrapper jar needed)
  MAVEN_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}/wrapper/dists/apache-maven-3.9.9"

  if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven 3.9.9 from $DISTRIBUTION_URL ..."
    mkdir -p "$MAVEN_HOME"
    TMPFILE="$(mktemp)"

    if command -v curl > /dev/null 2>&1; then
      curl -fsSL -o "$TMPFILE" "$DISTRIBUTION_URL"
    elif command -v wget > /dev/null 2>&1; then
      wget -q -O "$TMPFILE" "$DISTRIBUTION_URL"
    else
      echo "Error: Neither curl nor wget found. Install one to use the Maven wrapper." >&2
      exit 1
    fi

    # Extract
    if command -v unzip > /dev/null 2>&1; then
      unzip -q -o "$TMPFILE" -d "$MAVEN_HOME"
    elif command -v jar > /dev/null 2>&1; then
      (cd "$MAVEN_HOME" && jar xf "$TMPFILE")
    else
      echo "Error: Neither unzip nor jar found." >&2
      exit 1
    fi
    rm -f "$TMPFILE"
  fi

  MAVEN_CMD="$(find "$MAVEN_HOME" -name mvn -type f -path "*/bin/mvn" | head -1)"

  if [ ! -x "$MAVEN_CMD" ]; then
    chmod +x "$MAVEN_CMD" 2>/dev/null || true
  fi

  if [ ! -x "$MAVEN_CMD" ]; then
    echo "Error: Could not find mvn executable in $MAVEN_HOME" >&2
    exit 1
  fi

  # For Cygwin/MinGW, convert path
  if $cygwin; then
    MAVEN_PROJECTBASEDIR="$(cygpath --path --windows "$MAVEN_PROJECTBASEDIR")"
  fi

  exec "$MAVEN_CMD" \
    $MAVEN_OPTS \
    $MAVEN_DEBUG_OPTS \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    $MAVEN_CONFIG "$@"
fi
