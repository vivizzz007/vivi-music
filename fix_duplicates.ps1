$updates = @{
    "values\vivi_strings.xml" = @("slim")
    "values-it\vivi_strings.xml" = @("slim")
    "values-tr\vivi_strings.xml" = @("slim")
    "values-de\vivi_strings.xml" = @("slim")
    "values-ja\vivi_strings.xml" = @("slim")
    "values-es\vivi_strings.xml" = @("slim")
    "values-zh\vivi_strings.xml" = @("slim")
    "values-ru\vivi_strings.xml" = @("slim")
    "values-fr\vivi_strings.xml" = @("slim")
    "values-ar\vivi_strings.xml" = @("new_release_albums", "edit_lyrics", "appearance", "discord_integration")
    "values-pt-rBR\vivi_strings.xml" = @("new_release_albums", "edit_lyrics", "appearance", "discord_integration")
    "values-vi\vivi_strings.xml" = @("new_release_albums", "edit_lyrics", "appearance", "discord_integration")
    "values-in\vivi_strings.xml" = @("new_release_albums", "edit_lyrics", "appearance", "discord_integration")
}

foreach ($relPath in $updates.Keys) {
    echo "Processing $relPath"
    $fullPath = "d:\DEV\vivi\vivi-music\app\src\main\res\$relPath"
    if (Test-Path $fullPath) {
        $keys = $updates[$relPath]
        $content = Get-Content $fullPath
        $newContent = @()
        foreach ($line in $content) {
            $remove = $false
            foreach ($key in $keys) {
                if ($line -match "name=""$key""") {
                    $remove = $true
                    echo "  Removing duplicate: $key"
                    break
                }
            }
            if (-not $remove) {
                $newContent += $line
            }
        }
        $newContent | Set-Content $fullPath
    } else {
        echo "File not found: $fullPath"
    }
}
