# Ahorcado 1.0.1 - Ejecutable e instalador Windows

Esta version genera **dos entregables**:

1. Una aplicacion portable autocontenida, con su propio runtime Java:

```text
target\portable\Ahorcado\Ahorcado.exe
```

2. Un instalador nativo de Windows:

```text
target\installer\Ahorcado-1.0.1.exe
```

El usuario final **no necesita instalar Java, JavaFX, Maven ni WiX**.

## Requisitos solamente en la PC que crea el instalador

- Windows 10/11 de 64 bits.
- JDK 17 de 64 bits (recomendado Eclipse Temurin 17).
- `JAVA_HOME` apuntando al JDK 17 y el JDK agregado a `PATH`.
- WiX Toolset **3.x**, con `candle.exe` y `light.exe` disponibles en `PATH`.
- Conexion a Internet para la primera compilacion Maven.

> Importante: `jpackage` no hace cross-compilation. El EXE de Windows debe crearse ejecutando estos scripts en Windows.

## Paso 1 - Verificar requisitos

Ejecutar:

```bat
00-verificar-requisitos.bat
```

Debe finalizar con:

```text
TODO LISTO PARA CREAR LA VERSION WINDOWS
```

## Paso 2 - Crear y probar el EXE portable

Ejecutar:

```bat
01-crear-portable.bat
```

Luego probar:

```text
target\portable\Ahorcado\Ahorcado.exe
```

Ese archivo ya arranca el juego sin que Java este instalado en la PC de destino, siempre que se copie **toda la carpeta Ahorcado** que lo contiene.

Probar antes de continuar:

- Inicio de una partida.
- Sonidos de acierto/error y resultado.
- Las cuatro categorias.
- Preferencias de tiempo.
- Arriesgar.
- Como jugar.
- Acerca de.
- Cerrar y volver a abrir para verificar persistencia de preferencias y base local.

## Paso 3 - Crear el instalador

Ejecutar:

```bat
02-crear-instalador.bat
```

O, para hacer todo de una vez:

```bat
build-windows.bat
```

El instalador se crea en:

```text
target\installer
```

La configuracion del instalador incluye:

- Nombre: Ahorcado
- Version: 1.0.1
- Autor/Vendor: Mario Borelli
- Copyright: Copyright (c) 1996-2026 Mario Borelli
- Icono definitivo del personaje
- Acceso directo en el Escritorio
- Entrada en el menu Inicio
- Selector de carpeta de instalacion
- UUID de upgrade estable para futuras actualizaciones: `FEA50566-24F7-55D5-85B8-23BD83E4DB4A`

## Datos del usuario

La base de trabajo de cada usuario sigue almacenandose en:

```text
%LOCALAPPDATA%\Ahorcado\ahorcado.db
```

No se escribe dentro de la carpeta del programa. Por lo tanto una actualizacion o reinstalacion no deberia borrar los datos locales del usuario.

## Sobre Windows SmartScreen

Este instalador no esta firmado digitalmente. En una distribucion publica Windows puede mostrar `Editor desconocido` o una advertencia de SmartScreen. Eso no implica un fallo del programa: para evitar esa advertencia en una distribucion comercial/publica hace falta adquirir un certificado de firma de codigo y firmar el instalador.

## Solucion de problemas

### `jpackage was not found`

El `PATH` esta usando un JRE u otro Java. Verificar:

```bat
java -version
javac -version
jpackage --version
echo %JAVA_HOME%
```

### `candle` o `light` no encontrados

Agregar la carpeta `bin` de WiX Toolset 3.x al `PATH` y abrir una terminal nueva.

### Maven descarga dependencias en el primer build

Es normal. El proyecto usa `mvnw.cmd`, por lo que no hace falta instalar Maven manualmente.
