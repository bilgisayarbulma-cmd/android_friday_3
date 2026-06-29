#!/bin/sh
# Bu betik Android Studio'nun her projede otomatik olusturdugu standart
# gradlew dosyasidir. Gradle'in dogru surumunu (gradle-wrapper.properties'te
# tanimli) indirip kullanir, ayrica Gradle kurulu olmasi gerekmez.
APP_HOME=$(cd "$(dirname "$0")" && pwd)
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
