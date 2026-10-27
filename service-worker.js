const CACHE_NAME = "kocluk-pwa-v17";
const APP_SHELL = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./icon.svg",
  "./icon-180.png",
  "./icon-192.png",
  "./icon-512.png"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(APP_SHELL.map(url => new Request(url, { cache: "reload" }))))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(cacheNames => Promise.all(
        cacheNames
          .filter(cacheName => cacheName.startsWith("kocluk-pwa-") && cacheName !== CACHE_NAME)
          .map(cacheName => caches.delete(cacheName))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;

  const requestUrl = new URL(request.url);
  const isAppAsset = ["script", "style", "font", "image"].includes(request.destination);

  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request, { cache: "no-store" })
        .then(async response => {
          if (response.ok) {
            await caches.open(CACHE_NAME)
              .then(cache => cache.put("./index.html", response.clone()))
              .catch(error => console.error("Sayfa önbelleğe alınamadı:", error));
          }
          return response;
        })
        .catch(async error => {
          const cache = await caches.open(CACHE_NAME);
          const cachedResponse = await cache.match("./index.html") || await cache.match("./") || await cache.match(request);
          if (cachedResponse) return cachedResponse;
          throw error;
        })
    );
    return;
  }

  if (requestUrl.origin !== self.location.origin && !isAppAsset) return;

  event.respondWith(
    caches.open(CACHE_NAME).then(cache => cache.match(request)).then(cachedResponse => {
      if (cachedResponse) return cachedResponse;

      return fetch(request).then(response => {
        if (response.ok || response.type === "opaque") {
          return caches.open(CACHE_NAME)
            .then(cache => cache.put(request, response.clone()))
            .then(() => response)
            .catch(error => {
              console.error("Önbellek güncellenemedi:", error);
              return response;
            });
        }
        return response;
      });
    })
  );
});
