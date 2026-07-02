$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "E:\env\jdk\21"
$env:Path = "E:\env\jdk\21\bin;E:\env\maven\maven\maven\bin;$env:Path"

Push-Location "$PSScriptRoot\..\rbac-java"
try {
    & "E:\env\maven\maven\maven\bin\mvn.cmd" -q -DskipTests compile
}
finally {
    Pop-Location
}
