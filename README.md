<p align="center">
  <img src="src/main/resources/images/app-icon.png" alt="Ahorcado" width="140">
</p>

<h1 align="center">Ahorcado</h1>

<p align="center">
  El clásico juego del ahorcado, renovado para escritorio.
</p>

<p align="center">
  <strong>Versión 1.0.0</strong> · Windows · Linux
</p>

---

## Sobre el juego

**Ahorcado** es una versión de escritorio del clásico juego de palabras, desarrollada en Java y JavaFX.

El objetivo es descubrir la palabra o frase antes de agotar los intentos o el tiempo disponible. La aplicación funciona completamente **offline** y cuenta con distintas categorías, sonidos, animaciones y opciones de configuración.

## Características

- Más de **500 palabras y nombres** incluidos.
- Cuatro categorías:
  - Países
  - Ciudades
  - Marcas de autos
  - Bandas de Rock
- Tiempo configurable: **15, 30, 45 o 60 segundos**.
- Posibilidad de seleccionar letras o **arriesgar la respuesta completa**.
- Manejo de acentos, diéresis, eñes y otros caracteres especiales.
- Sonidos para errores, victoria y derrota.
- Animaciones e interfaz gráfica moderna.
- Preferencias persistentes entre partidas.
- Ayuda integrada dentro de la aplicación.
- Base de datos SQLite incluida.
- Funcionamiento completamente offline.
- No requiere tener Java instalado en la computadora del usuario.

## Capturas de pantalla

<p align="center">
  <img src="docs/screenshot-main.png" alt="Ahorcado - Pantalla principal" width="520">
</p>

<p align="center">
  <em>Pantalla principal del juego.</em>
</p>

<br>

<p align="center">
  <img src="docs/screenshot-time.png" alt="Ahorcado - Partida con tiempo configurable" width="520">
</p>

<p align="center">
  <em>Partida con límite de tiempo configurable.</em>
</p>

## Descargas

Las versiones compiladas se publican en la sección **Releases** del repositorio.

### Windows

Archivo:

`Ahorcado-1.0.0.exe`

Instalador autocontenido para Windows de 64 bits.

El instalador incluye el runtime necesario, por lo que **no es necesario instalar Java**.

### Linux

Archivo:

`ahorcado_1.0.0-2_amd64.deb`

Paquete para sistemas Linux x86-64 basados en Debian, incluyendo Debian, Ubuntu, Linux Mint y distribuciones derivadas.

Incluye su propio runtime Java.

---

# Instalación

## Windows

1. Descargá `Ahorcado-1.0.0.exe` desde la sección **Releases**.
2. Hacé doble clic sobre el archivo descargado.
3. Seguí los pasos del instalador.
4. Al finalizar, podrás ejecutar **Ahorcado** desde el menú Inicio o desde el acceso directo creado por el instalador.

### Advertencia de Microsoft Defender SmartScreen

Esta primera versión no utiliza un certificado comercial de firma de código, por lo que Windows puede mostrar una advertencia de Microsoft Defender SmartScreen.

Si descargaste Ahorcado desde este repositorio oficial:

1. Seleccioná **Más información**.
2. Elegí **Ejecutar de todas formas**.

## Linux

### Instalación gráfica

En la mayoría de las distribuciones compatibles:

1. Descargá `ahorcado_1.0.0-2_amd64.deb`.
2. Hacé doble clic sobre el archivo.
3. Abrilo con el instalador de software de tu distribución.
4. Seleccioná **Instalar**.
5. Introducí tu contraseña si el sistema la solicita.

Una vez instalado, **Ahorcado aparecerá en el menú de aplicaciones**.

### Instalación desde Terminal

Abrí una terminal en la carpeta donde descargaste el archivo y ejecutá:

```bash
sudo apt install ./ahorcado_1.0.0-2_amd64.deb
```

Es importante mantener el `./` delante del nombre del archivo para indicar que se trata de un paquete local.

### Desinstalación

Para eliminar Ahorcado:

```bash
sudo apt remove ahorcado
```

---

## Requisitos del sistema

### Windows

- Windows de 64 bits.
- Arquitectura x86-64.
- No requiere una instalación independiente de Java.

### Linux

- Linux x86-64.
- Debian, Ubuntu, Linux Mint o una distribución compatible con paquetes `.deb`.
- No requiere una instalación independiente de Java.

### Pantalla

La interfaz de Ahorcado está pensada para utilizarse en una ventana de escritorio de aproximadamente **480 × 785 píxeles**.

Para visualizarla cómodamente, se recomienda:

- una resolución de pantalla de **1280 × 900 o superior**;
- disponer de aproximadamente **800 píxeles de altura útil** para la ventana.

En pantallas con menor altura disponible, la interfaz puede quedar ajustada por las barras y decoraciones del sistema operativo.

---

## Compilar desde el código fuente

### Requisitos generales

- JDK 17
- Maven Wrapper incluido en el proyecto

### Windows

Para generar la versión portable y el instalador:

```bat
00-verificar-requisitos.bat
01-crear-portable.bat
02-crear-instalador.bat
```

También puede realizarse todo el proceso con:

```bat
build-windows.bat
```

Para generar el instalador de Windows se utiliza **WiX Toolset 3.14**.

### Linux

Dar permisos de ejecución a los scripts:

```bash
chmod +x *.sh mvnw
```

Luego:

```bash
./00-verificar-requisitos-linux.sh
./01-crear-portable-linux.sh
./02-crear-deb.sh
```

También puede realizarse todo el proceso con:

```bash
./build-linux.sh
```

Para generar el paquete `.deb` se requieren las herramientas estándar de empaquetado de Debian, incluyendo `dpkg-deb` y `fakeroot`.

---

## Tecnologías utilizadas

- Java 17
- JavaFX
- SQLite
- Maven
- jpackage
- WiX Toolset
- CSS

## Estructura principal

```text
src/
├── main/
│   ├── java/
│   │   └── com/llorieb/ahorcado/
│   └── resources/
│       ├── css/
│       ├── images/
│       ├── sonidos/
│       ├── AhorcadoLayout.fxml
│       └── ahorcado.db
```

## Funcionamiento offline

Ahorcado funciona completamente **offline**.

No necesita conexión a Internet para jugar y la base de palabras está incluida en la aplicación.

## Autor

**Mario Borelli**

Copyright © 1996–2026 Mario Borelli
