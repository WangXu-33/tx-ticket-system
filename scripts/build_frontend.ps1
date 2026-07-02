$ErrorActionPreference = "Stop"

$env:Path = "E:\env\node\nvm\v20.18.1;$env:Path"

Push-Location "$PSScriptRoot\..\rbac-vue"
try {
    pnpm build
}
finally {
    Pop-Location
}
