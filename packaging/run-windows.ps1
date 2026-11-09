$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$usersFile = Join-Path $PSScriptRoot "users.xml"
if (-not (Test-Path $usersFile)) {
    $usersTemplate = @'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE users SYSTEM "users.dtd">
<users xsi:noNamespaceSchemaLocation="users.xsd"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <user>
        <username>admin</username>
        <displayName>Administrator</displayName>
        <roles/>
    </user>
</users>
'@
    [System.IO.File]::WriteAllText(
        $usersFile,
        $usersTemplate,
        [System.Text.UTF8Encoding]::new($false)
    )
}

$usersXml = Get-Content -Raw $usersFile
$userCount = [regex]::Matches($usersXml, "<username>").Count
$passwordHashCount = [regex]::Matches($usersXml, "<passwordHash>").Count
$hadInitialPassword = -not [string]::IsNullOrWhiteSpace($env:AUTH_INITIAL_PASSWORD)

if ($passwordHashCount -lt $userCount -and -not $hadInitialPassword) {
    $securePassword = Read-Host "Initial user password" -AsSecureString
    if ($securePassword.Length -eq 0) {
        throw "The initial password must not be empty."
    }

    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:AUTH_INITIAL_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

try {
    & (Join-Path $PSScriptRoot "TriviaThrone.exe") @args
    exit $LASTEXITCODE
}
finally {
    if (-not $hadInitialPassword) {
        Remove-Item Env:AUTH_INITIAL_PASSWORD -ErrorAction SilentlyContinue
    }
}
