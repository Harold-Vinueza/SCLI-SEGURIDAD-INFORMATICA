// /src/main/resources/static/js/client-info.js
(function () {
    // ===================== HELPERS =====================
    const norm = (s) => (s || "").toString().trim();
    const low  = (s) => norm(s).toLowerCase();

    async function getPublicIpFallback() {
        // Intenta 2 servicios simples; si ambos fallan, devuelve null
        try {
            const r = await fetch("https://api.ipify.org?format=json", { cache: "no-store" });
            if (r.ok) { const j = await r.json(); return j.ip || null; }
        } catch (_) {}
        try {
            const r2 = await fetch("https://ifconfig.me/ip", { cache: "no-store" });
            if (r2.ok) { const t = (await r2.text() || "").trim(); return t || null; }
        } catch (_) {}
        return null;
    }

    function normalizeBrand(brand) {
        if (!brand) return null;
        const b = brand.toLowerCase();
        if (b.includes("edge") || b.includes("edg")) return "Microsoft Edge";
        if (b.includes("chrome"))  return "Chrome";
        if (b.includes("chromium"))return "Chromium";
        if (b.includes("firefox")) return "Firefox";
        if (b.includes("safari"))  return "Safari";
        if (b.includes("opera") || b.includes("opr")) return "Opera";
        if (b.includes("brave"))   return "Brave";
        return brand; // deja desconocidos tal cual
    }

    // Fallback para navegadores sin UA-CH (Firefox/Safari/etc.)
    function parseUA(uaRaw) {
        const ua = uaRaw || "";
        const L  = ua.toLowerCase();

        // Edge (Chromium)
        let m = ua.match(/\bEdgA?\/([\d.]+)/);
        if (m) return { browser: "Microsoft Edge", browserVer: m[1] };

        // Opera
        m = ua.match(/\bOPR\/([\d.]+)/);
        if (m) return { browser: "Opera", browserVer: m[1] };

        // Brave: normalmente cae como Chrome (si luego detectas window.brave puedes cambiar)
        // Chrome (evita confundir con Chromium)
        m = ua.match(/\bChrome\/([\d.]+)/);
        if (m && !L.includes("chromium")) return { browser: "Chrome", browserVer: m[1] };

        // Chromium
        m = ua.match(/\bChromium\/([\d.]+)/);
        if (m) return { browser: "Chromium", browserVer: m[1] };

        // Firefox
        m = ua.match(/\bFirefox\/([\d.]+)/);
        if (m) return { browser: "Firefox", browserVer: m[1] };

        // Safari (Version/x.y)
        if (L.includes("safari") && !L.includes("chrome")) {
            m = ua.match(/\bVersion\/([\d.]+)/);
            return { browser: "Safari", browserVer: m ? m[1] : null };
        }

        return { browser: null, browserVer: null };
    }

    // ===================== PAYLOAD =====================
    async function buildPayload() {
        const ua = navigator.userAgent || "";

        let hints = null, browser = null, browserVer = null,
            platform = null, platformVer = null, arch = null,
            model = null, deviceType = null;

        // UA-CH (cuando existe)
        if (navigator.userAgentData && navigator.userAgentData.getHighEntropyValues) {
            try {
                hints = await navigator.userAgentData.getHighEntropyValues([
                    "platform", "platformVersion", "architecture", "model",
                    "uaFullVersion", "fullVersionList", "bitness", "wow64"
                ]);

                if (Array.isArray(hints.fullVersionList) && hints.fullVersionList.length) {
                    const b = hints.fullVersionList[0];
                    browser    = normalizeBrand(b.brand);
                    browserVer = b.version || null;
                }
                platform    = hints.platform || null;
                platformVer = hints.platformVersion || null;
                arch        = hints.architecture || null;
                model       = hints.model || null;
                deviceType  = (platform && /android|ios/i.test(platform)) ? "mobile" : "desktop";
            } catch (_) { /* sigue con fallback */ }
        }

        // Fallback UA clásico
        if (!browser) {
            const p = parseUA(ua);
            browser    = p.browser;
            browserVer = p.browserVer;
        }
        if (!platform) {
            const P = (navigator.platform || "").toLowerCase();
            if (P.includes("win")) platform = "Windows";
            else if (P.includes("mac")) platform = "macOS";
            else if (P.includes("linux")) platform = "Linux";
        }

        // IP pública real (si tu proxy ya usa X-Forwarded-For, el backend la tomará; esto es refuerzo)
        const ipPublic = await getPublicIpFallback();

        return {
            ua,
            uaHints: hints || null,
            ipPublic: ipPublic || null,
            ipLocal: null,               // si luego implementas WebRTC para LAN
            browser: browser || null,
            browserVer: browserVer || null,
            platform: platform || null,
            platformVer: platformVer || null,
            arch: arch || null,
            deviceModel: model || null,
            deviceType: deviceType || null
        };
    }

    // ===================== ENVÍOS =====================
    async function postClientInfo() {
        try {
            const payload = await buildPayload();
            payload.event = "start"; // marca explícita de inicio (o "ping")
            await fetch("/client-info", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify(payload)
            });
        } catch (e) {
            // no rompas la UI por esto
            console.warn("client-info start:", e);
        }
    }

    // Registro de fin de sesión (recuperado del primer enfoque)
    let __endSent = false;
    async function sendEndEvent() {
        if (__endSent) return;
        __endSent = true;
        try {
            const payload = await buildPayload();
            payload.event = "end"; // el backend debe tratar este evento como fin de sesión
            const data = JSON.stringify(payload);

            // Preferir sendBeacon para no perder el evento al cerrar
            if (navigator.sendBeacon) {
                const blob = new Blob([data], { type: "application/json" });
                navigator.sendBeacon("/client-info", blob);
            } else {
                // Respaldo: fetch keepalive
                fetch("/client-info", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    keepalive: true,
                    body: data
                }).catch(() => {});
            }
        } catch (e) {
            // última línea de defensa
            try {
                navigator.sendBeacon && navigator.sendBeacon("/client-info", new Blob([JSON.stringify({ event: "end" })], { type: "application/json" }));
            } catch (_) {}
        }
    }

    function registerEndHooks() {
        // Dispara al ocultar, cambiar de página o cerrar la pestaña
        document.addEventListener("visibilitychange", () => {
            if (document.visibilityState === "hidden") sendEndEvent();
        }, { capture: true });

        // iOS/Safari suelen respetar pagehide mejor que beforeunload
        window.addEventListener("pagehide", sendEndEvent, { capture: true });
        // Respaldo adicional
        window.addEventListener("beforeunload", sendEndEvent, { capture: true });
    }

    // ===================== EXPORTS =====================
    // Llamas __postClientInfo() cuando te convenga (por ejemplo, al cargar la página)
    window.__postClientInfo = postClientInfo;

    // Registra los hooks de fin apenas carga el script (no interfiere si no cierras la pestaña)
    registerEndHooks();
})();