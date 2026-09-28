# Ahorcado 1.0.1 - Build para Linux

Esta version permite generar en Linux dos formatos de distribucion:

1. **Portable autocontenido** (`app-image` + `.tar.gz`).
2. **Instalador `.deb`** para Debian, Ubuntu, Linux Mint y derivados.

El usuario final no necesita tener Java instalado: `jpackage` incluye un runtime reducido dentro de Ahorcado.

## Importante

Los paquetes Linux deben construirse en Linux. `jpackage` no hace cross-compilation desde Windows.

## Requisitos

- Linux x64 o arquitectura compatible con el JDK/JavaFX usado para compilar.
- JDK 17 o posterior con `javac` y `jpackage`.
- Conexion a Internet en el primer build para que Maven Wrapper descargue Maven y las dependencias.
- Para crear `.deb`: `fakeroot` y `dpkg-deb`.

En Debian/Ubuntu/Mint:

```bash
sudo apt update
sudo apt install fakeroot
```

Maven no necesita instalarse manualmente: el proyecto contiene `./mvnw`.

## Paso 0 - Dar permisos a los scripts

Si el ZIP no conserva permisos ejecutables:

```bash
chmod +x 00-verificar-requisitos-linux.sh \
         01-crear-portable-linux.sh \
         02-crear-deb.sh \
         build-linux.sh \
         mvnw
```

## Paso 1 - Verificar requisitos

```bash
./00-verificar-requisitos-linux.sh
```

Debe terminar con:

```text
TODO LISTO PARA CREAR LA VERSION LINUX
```

## Paso 2 - Crear y probar el portable

```bash
./01-crear-portable-linux.sh
```

Genera:

```text
target/portable-linux/Ahorcado/bin/Ahorcado
```

Ejecutarlo directamente:

```bash
./target/portable-linux/Ahorcado/bin/Ahorcado
```

Tambien genera un `.tar.gz` autocontenido en `target/portable-linux/`. Ese archivo es util como descarga Linux generica y como alternativa para quienes no usan Debian/Ubuntu.

## Paso 3 - Crear instalador .deb

Despues de probar el portable:

```bash
./02-crear-deb.sh
```

El instalador queda en:

```text
target/installer-linux/
```

Su nombre sera similar a:

```text
ahorcado_1.0.1-1_amd64.deb
```

## Instalar el .deb

Desde la carpeta del proyecto:

```bash
sudo apt install ./target/installer-linux/*.deb
```

El paquete agrega Ahorcado al menu de aplicaciones y usa el mismo icono del personaje.

Ademas, el lanzador incluye:

```text
StartupWMClass=com.llorieb.ahorcado.Main
```

Esto hace que GNOME asocie correctamente la ventana JavaFX en ejecucion con Ahorcado y muestre el icono del personaje en el dock/barra de tareas, en lugar del icono generico de engranaje.

## Actualizar desde el paquete 1.0.0-2

La version `1.0.1-1` puede instalarse directamente sobre `1.0.0-2`:

```bash
sudo apt install ./target/installer-linux/ahorcado_1.0.1-1_amd64.deb
```

No es necesario desinstalar primero Ahorcado.

## Desinstalar

```bash
sudo apt remove ahorcado
```

La base de datos y preferencias del usuario se guardan en su directorio personal, de forma independiente de los archivos instalados de la aplicacion.

## Build completo

Para ejecutar portable + instalador en una sola operacion:

```bash
./build-linux.sh
```

## RPM

`jpackage` tambien puede generar `.rpm`, pero requiere `rpm-build` y conviene producirlo en una distribucion RPM (Fedora/RHEL/openSUSE). Para el MVP se distribuye `.deb` mas el `.tar.gz` portable, que cubren el caso principal y ofrecen una alternativa para otras distribuciones.
