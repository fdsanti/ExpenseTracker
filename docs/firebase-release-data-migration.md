# Firebase RTDB migration pipeline

Guia unica para preparar un export JSON de Firebase Realtime Database antes de probarlo en debug o subirlo a release.

Esta guia unifica el proceso que se venia haciendo manualmente con los exports:

- Limpiar ramas top-level viejas.
- Conservar `home_index` y `trackers_v2`.
- Agregar o validar `users`.
- Agregar o validar `households/default/defaultParticipants`.
- Asociar participantes historicos por `userId`.
- Eliminar `participants/{p1|p2}/name` cuando ya existe `userId`.
- Guardar el JSON en UTF-8 sin BOM.
- Validar reglas de importacion de Firebase.

Para cada corrida, usar los UIDs reales del proyecto Firebase destino. Debug y release pueden tener UIDs distintos aunque el login sea con la misma cuenta de Google.

## Objetivo final

El JSON final debe quedar listo para importar en Firebase y debe tener estas ramas top-level:

```text
home_index
trackers_v2
users
households
```

No deben quedar ramas legacy top-level como:

```text
DATA1
DATA2
DATAxx
allTables
settings
```

## Modelo mental

La app separa identidad global, configuracion del grupo y datos historicos del tracker.

- `users`: perfil global de cada usuario autenticado.
- `households`: configuracion del grupo/casa y participantes default para trackers nuevos.
- `home_index`: indice liviano para cargar la home rapido.
- `trackers_v2`: datos completos de cada tracker.

Los gastos apuntan a `participantId` (`p1` o `p2`), no directo a `userId`. Cada tracker conserva su propia historia de participantes. El `userId` dentro del participante permite saber que usuario real ocupa ese slot.

## Datos a confirmar antes de procesar

1. Export JSON mas reciente que se quiere preparar.
2. Proyecto destino:
   - `debug`, para pruebas.
   - `release`, para produccion.
3. UID real de Fede en el proyecto destino.
4. UID real de Clari en el proyecto destino.
5. Nicknames esperados:
   - Fede.
   - Clari.
6. Si se va a copiar `users` desde otro archivo, confirmar que esos UIDs pertenecen al proyecto destino.

Si hay participantes ambiguos o UIDs dudosos, frenar antes de generar el archivo final.

## UIDs por entorno

Cuando se procese un export nuevo, confirmar si el archivo final se quiere para probar en debug o para subir a release.

### Debug

```text
Fede:  LSXxjd6P9DNcM8Hv7Shw10J9k3c2
Clari: 45j3v3MyyDYyMQVFaFcP0ZMgdiG2
```

### Release

```text
Fede:  vfjpl8ryD0ZhlidXf2T5w8qoms92
Clari: 6x0r9W7o8xguX8PbVNpu248lQbq1
```

## Estructura esperada

### `users`

Fuente global del nombre visible.

```json
"users": {
  "<uid-fede>": {
    "nickname": "Fede",
    "email": "optional@example.com",
    "updatedAt": "2026-08-01"
  },
  "<uid-clari>": {
    "nickname": "Clari",
    "email": "optional@example.com",
    "updatedAt": "2026-08-01"
  }
}
```

Reglas:

- `nickname` es requerido para mostrar nombres en la app.
- `email` es opcional. Puede faltar si el usuario fue creado por migracion manual o no guardo su perfil desde la app.
- Preservar campos existentes como `email` y `updatedAt` salvo que haya una razon clara para cambiarlos.

### `households`

Configuracion del grupo/casa. Hoy se usa un household unico llamado `default`.

```json
"households": {
  "default": {
    "defaultParticipants": {
      "p1": {
        "active": true,
        "order": 1,
        "userId": "<uid-fede>"
      },
      "p2": {
        "active": true,
        "order": 2,
        "userId": "<uid-clari>"
      }
    }
  }
}
```

Reglas:

- `households/default/defaultParticipants` define los participantes por defecto para trackers nuevos.
- El orden `p1`/`p2` aca es solo default. No implica que en todos los trackers historicos Fede sea siempre `p1`.
- No guardar `name` en `defaultParticipants`. El nombre visible sale de `users/{uid}/nickname`.

### `home_index`

Indice liviano para la home.

Preservar en general estos campos:

- `trackerId`
- `name`
- `createdAt`
- `closed`
- `isSetupComplete`
- `type`
- `monthKey`, cuando exista
- `totalAmount`
- `summaryVersion`

No copiar nombres de usuarios ni UIDs a `home_index`.

### `trackers_v2`

Fuente completa de cada tracker.

Estructura esperada:

```json
"trackers_v2": {
  "DATA79": {
    "meta": {},
    "participants": {},
    "categories": {},
    "expenses": {},
    "summary": {}
  }
}
```

#### `participants`

Modelo objetivo:

```json
"participants": {
  "p1": {
    "active": true,
    "income": 1000,
    "incomePending": false,
    "order": 1,
    "userId": "<uid>"
  }
}
```

Reglas:

- Mantener `p1` y `p2`: los gastos dependen de esos ids.
- Cada participante real debe tener `userId`.
- Preservar `income` historico.
- Si ya tenia sueldo historico, setear `incomePending: false`.
- Si falta `order`, inferir por clave:
  - `p1` -> `1`
  - `p2` -> `2`
- No reordenar participantes historicos.
- Si un participante tiene `userId`, borrar `name`.
- Si un participante no tiene `userId`, preservar `name` como fallback legacy y frenar para revisar manualmente.

No borrar otros `name` que no sean de participantes:

- `users/{uid}/nickname`
- `home_index/{trackerId}/name`
- `trackers_v2/{trackerId}/meta/name`
- `trackers_v2/{trackerId}/categories/{categoryId}/name`

## Pipeline completo

### 1. Analizar el export original

Leer el archivo sin modificarlo y reportar:

- Claves top-level.
- Cantidad de trackers en `trackers_v2`.
- Cantidad total de participantes.
- Participantes con `userId`.
- Participantes sin `userId`.
- Participantes con `name`.
- Nombres legacy unicos en participantes.
- Usuarios presentes en `users`.
- Si existe `households/default/defaultParticipants`.

### 2. Crear una copia filtrada

Crear un archivo nuevo. Nunca pisar el export original.

Conservar:

- `home_index`
- `trackers_v2`
- `users`, si existe y pertenece al proyecto destino
- `households`, si existe y pertenece al proyecto destino

Eliminar top-level legacy:

- `DATAxx`
- `allTables`
- `settings`
- Cualquier otra rama que no forme parte del modelo final

### 3. Asegurar `users`

Si el export no trae `users`, crear o copiar la rama desde una fuente confiable del mismo proyecto destino.

Validar:

- Existen los dos UIDs esperados.
- Cada UID tiene `nickname`.
- No se copiaron UIDs de otro proyecto por error.

### 4. Asegurar `households`

Crear o actualizar:

```text
households/default/defaultParticipants/p1/userId
households/default/defaultParticipants/p2/userId
```

Usar los UIDs del proyecto destino.

No agregar `name` en `defaultParticipants`.

### 5. Normalizar participantes historicos

Para cada tracker:

1. Revisar `trackers_v2/{trackerId}/participants`.
2. Si el participante ya tiene `userId`, preservarlo.
3. Si no tiene `userId`, usar `name` legacy solo para identificar:
   - Variantes inequivocas de Fede -> UID de Fede.
   - Variantes inequivocas de Clari/Clara -> UID de Clari.
4. Si el nombre es ambiguo, no asumir. Frenar y consultar.
5. Preservar `income`, `active` y `order`.
6. Agregar `incomePending: false` cuando ya hay sueldo historico.
7. Borrar `name` una vez que el participante tenga `userId`.

### 6. Guardar como UTF-8 sin BOM

Firebase puede rechazar un JSON visualmente valido si la codificacion no es la esperada.

El archivo final debe escribirse como:

- JSON parseable.
- Pretty-printed.
- UTF-8 sin BOM.

### 7. Validar para Firebase

Validar en todos los niveles:

- No hay claves vacias.
- Ninguna clave contiene:
  - `.`
  - `$`
  - `#`
  - `[`
  - `]`
  - `/`
- Ninguna clave supera 768 bytes en UTF-8.
- El archivo parsea al leerlo como UTF-8.
- `HasUtf8Bom` es `False`.

### 8. Validar modelo final

El resumen esperado debe confirmar:

- Top-level keys: `home_index`, `trackers_v2`, `users`, `households`.
- No hay top-level `DATAxx`.
- No hay `allTables`.
- No hay `settings`.
- `users` contiene Fede y Clari.
- `households/default/defaultParticipants` contiene `p1` y `p2` con `userId`.
- Todos los participantes reales tienen `userId`.
- Cero participantes con `userId` tienen `name`.
- Participantes sin `userId`: `0`.
- Ambiguos: `0`.
- Claves Firebase invalidas: `0`.
- UTF-8 BOM: `False`.

## Script base

Este script es una referencia. Ajustar rutas y UIDs en cada corrida.

```powershell
$source = 'C:\ruta\export-original.json'
$dest = 'C:\ruta\export-final.firebase.json'

$uidFede = '<uid-fede-del-proyecto-destino>'
$uidClari = '<uid-clari-del-proyecto-destino>'

$root = Get-Content -LiteralPath $source -Raw | ConvertFrom-Json

$final = [ordered]@{}
$final['home_index'] = $root.home_index
$final['trackers_v2'] = $root.trackers_v2

if ($root.PSObject.Properties.Name -contains 'users') {
  $final['users'] = $root.users
} else {
  $final['users'] = [ordered]@{
    $uidFede = [ordered]@{ nickname = 'Fede' }
    $uidClari = [ordered]@{ nickname = 'Clari' }
  }
}

$final['households'] = [ordered]@{
  default = [ordered]@{
    defaultParticipants = [ordered]@{
      p1 = [ordered]@{
        active = $true
        order = 1
        userId = $uidFede
      }
      p2 = [ordered]@{
        active = $true
        order = 2
        userId = $uidClari
      }
    }
  }
}

$ambiguous = New-Object System.Collections.Generic.List[object]
$participantCount = 0
$assignedUserIds = 0
$removedNames = 0

foreach ($tracker in $final.trackers_v2.PSObject.Properties) {
  $trackerId = $tracker.Name
  $participants = $tracker.Value.participants
  if ($null -eq $participants) { continue }

  foreach ($participant in $participants.PSObject.Properties) {
    $participantCount++
    $participantId = $participant.Name
    $value = $participant.Value
    $userId = $value.userId
    $hasUserId = $null -ne $userId -and -not [string]::IsNullOrWhiteSpace([string]$userId)

    if (-not $hasUserId) {
      $legacyName = [string]$value.name
      $normalizedName = $legacyName.Trim().ToLowerInvariant()

      if ($normalizedName -eq 'fede') {
        $value | Add-Member -NotePropertyName userId -NotePropertyValue $uidFede -Force
        $hasUserId = $true
        $assignedUserIds++
      } elseif ($normalizedName -eq 'clari' -or $normalizedName -eq 'clara') {
        $value | Add-Member -NotePropertyName userId -NotePropertyValue $uidClari -Force
        $hasUserId = $true
        $assignedUserIds++
      } else {
        $ambiguous.Add([pscustomobject]@{
          TrackerId = $trackerId
          ParticipantId = $participantId
          Name = $legacyName
        })
      }
    }

    if (-not ($value.PSObject.Properties.Name -contains 'order')) {
      $order = if ($participantId -eq 'p2') { 2 } else { 1 }
      $value | Add-Member -NotePropertyName order -NotePropertyValue $order -Force
    }

    if ($hasUserId -and ($value.PSObject.Properties.Name -contains 'name')) {
      $value.PSObject.Properties.Remove('name')
      $removedNames++
    }

    if ($hasUserId -and ($value.PSObject.Properties.Name -contains 'income')) {
      $value | Add-Member -NotePropertyName incomePending -NotePropertyValue $false -Force
    }
  }
}

if ($ambiguous.Count -gt 0) {
  $ambiguous | Format-Table -AutoSize
  throw 'Hay participantes ambiguos. Revisar antes de generar el archivo final.'
}

$text = $final | ConvertTo-Json -Depth 100
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($dest, $text, $utf8NoBom)

[pscustomobject]@{
  Path = $dest
  Participants = $participantCount
  AssignedUserIds = $assignedUserIds
  RemovedParticipantNames = $removedNames
  SizeBytes = (Get-Item -LiteralPath $dest).Length
} | Format-List
```

## Script de validacion

Actualizar `$path` con el archivo generado.

```powershell
$path = 'C:\ruta\export-final.firebase.json'

$invalid = New-Object System.Collections.Generic.List[object]
$tooLong = New-Object System.Collections.Generic.List[object]
$utf8 = [System.Text.Encoding]::UTF8

function Test-Keys($node, [string]$path) {
  if ($null -eq $node) { return }

  if ($node -is [pscustomobject]) {
    foreach ($prop in $node.PSObject.Properties) {
      $keyText = $prop.Name

      if ($keyText.Length -eq 0 -or $keyText -match '[\.\$#\[\]/]') {
        $script:invalid.Add([pscustomobject]@{ Path = $path; Key = $keyText })
      }

      if ($script:utf8.GetByteCount($keyText) -gt 768) {
        $script:tooLong.Add([pscustomobject]@{
          Path = $path
          KeyBytes = $script:utf8.GetByteCount($keyText)
          Key = $keyText
        })
      }

      Test-Keys $prop.Value ($path + '/' + $keyText)
    }
  } elseif ($node -is [System.Collections.IEnumerable] -and $node -isnot [string]) {
    $i = 0
    foreach ($item in $node) {
      Test-Keys $item ($path + '[' + $i + ']')
      $i++
    }
  }
}

$bytes = [System.IO.File]::ReadAllBytes($path)
$text = [System.Text.Encoding]::UTF8.GetString($bytes)
$json = $text | ConvertFrom-Json

Test-Keys $json ''

$dataTopLevelCount = @($json.PSObject.Properties.Name | Where-Object { $_ -match '^DATA\d+$' }).Count
$participants = 0
$participantUserIds = 0
$participantNamesWithUserId = 0
$participantsWithoutUserId = 0

foreach ($tracker in $json.trackers_v2.PSObject.Properties) {
  $participantsNode = $tracker.Value.participants
  if ($null -eq $participantsNode) { continue }

  foreach ($participant in $participantsNode.PSObject.Properties) {
    $participants++
    $value = $participant.Value
    $uid = $value.userId
    $hasUserId = $null -ne $uid -and -not [string]::IsNullOrWhiteSpace([string]$uid)
    $hasName = $value.PSObject.Properties.Name -contains 'name'

    if ($hasUserId) { $participantUserIds++ } else { $participantsWithoutUserId++ }
    if ($hasUserId -and $hasName) { $participantNamesWithUserId++ }
  }
}

[pscustomobject]@{
  TopLevelKeys = ($json.PSObject.Properties.Name -join ', ')
  DataTopLevelCount = $dataTopLevelCount
  HasUsers = ($json.PSObject.Properties.Name -contains 'users')
  HasHouseholds = ($json.PSObject.Properties.Name -contains 'households')
  Participants = $participants
  ParticipantUserIds = $participantUserIds
  ParticipantsWithoutUserId = $participantsWithoutUserId
  ParticipantNamesWithUserId = $participantNamesWithUserId
  InvalidKeyCount = $invalid.Count
  TooLongKeyCount = $tooLong.Count
  HasUtf8Bom = ($bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191)
  SizeBytes = $bytes.Length
} | Format-List
```

## Checklist antes de importar

- Backup completo de RTDB destino.
- UIDs confirmados para el proyecto destino.
- JSON final generado en archivo nuevo.
- `DataTopLevelCount : 0`.
- `HasUsers : True`.
- `HasHouseholds : True`.
- `ParticipantsWithoutUserId : 0`.
- `ParticipantNamesWithUserId : 0`.
- `InvalidKeyCount : 0`.
- `TooLongKeyCount : 0`.
- `HasUtf8Bom : False`.
- Probar primero en debug si el cambio va a release.

## Checklist manual en la app

Despues de importar en debug:

1. Abrir home y verificar que carga rapido.
2. Entrar a trackers historicos y verificar gastos, resumen y saldos.
3. Verificar que los nombres visibles salen bien.
4. Crear un gasto nuevo.
5. Editar un gasto viejo.
6. Cambiar apodo y confirmar que solo cambia `users/{uid}/nickname`.
7. Editar sueldo y confirmar que no se vuelve a escribir `participants/name`.
8. Crear tracker nuevo y confirmar que nace con participantes por `userId`.
9. Probar login con ambos usuarios.

## Resultado esperado para reportar

Al terminar cada corrida, reportar:

```text
Entrada:
Salida:
Proyecto destino:
Top-level keys:
Trackers:
Participantes:
Participantes con userId:
Participantes sin userId:
participants/name eliminados:
Ambiguos:
Users:
Household default participants:
Invalid Firebase keys:
UTF-8 BOM:
```
