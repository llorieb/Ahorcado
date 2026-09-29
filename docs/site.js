(() => {
  const WINDOWS_FILE = "Ahorcado-1.0.1.exe";
  const LINUX_FILE = "ahorcado_1.0.1-1_amd64.deb";
  const VERSION = "v1.0.1";

  function githubCoordinates() {
    const host = window.location.hostname.toLowerCase();
    const parts = window.location.pathname.split("/").filter(Boolean);

    if (host.endsWith(".github.io")) {
      const owner = host.split(".")[0];
      const repo = parts.length ? parts[0] : `${owner}.github.io`;
      return { owner, repo };
    }

    return {
      owner: "TU_USUARIO",
      repo: "TU_REPOSITORIO"
    };
  }

  const { owner, repo } = githubCoordinates();
  const repoUrl = `https://github.com/${owner}/${repo}`;
  const releaseUrl = `${repoUrl}/releases/tag/${VERSION}`;
  const assetBase = `${repoUrl}/releases/download/${VERSION}`;

  const links = {
    "download-windows": `${assetBase}/${WINDOWS_FILE}`,
    "download-windows-bottom": `${assetBase}/${WINDOWS_FILE}`,
    "download-linux": `${assetBase}/${LINUX_FILE}`,
    "download-linux-bottom": `${assetBase}/${LINUX_FILE}`,
    "release-link": releaseUrl,
    "footer-release": releaseUrl,
    "nav-source": repoUrl,
    "footer-source": repoUrl
  };

  Object.entries(links).forEach(([id, url]) => {
    const element = document.getElementById(id);
    if (element) element.href = url;
  });

  if (owner === "TU_USUARIO") {
    document.querySelectorAll('[id^="download-"]').forEach(el => {
      el.addEventListener("click", event => {
        event.preventDefault();
        alert("Los enlaces de descarga se completarán automáticamente al publicar la página en GitHub Pages.");
      });
    });
  }
})();
