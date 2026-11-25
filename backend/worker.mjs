// Cloudflare Worker. Store OPENROUTER_API_KEY as a Cloudflare secret, never in this file.
const MODEL = "nvidia/nemotron-3.5-super-vl:free";
const response = (status, body) => new Response(JSON.stringify(body), {
    status, headers: {"Content-Type": "application/json", "Cache-Control": "no-store"}
});
const recent = new Map();
async function limited(request, env) {
    const ip = request.headers.get("CF-Connecting-IP") || "local";
    if (env.RATE_LIMITER) return !(await env.RATE_LIMITER.limit({key: ip})).success;
    // Dashboard-only deployments: conservative per-isolate fallback.
    // Use the Wrangler binding for Cloudflare-managed rate limiting.
    const now = Date.now();
    for (const [key, value] of recent) if (value.until <= now) recent.delete(key);
    const entry = recent.get(ip) || {count: 0, until: now + 60000};
    if (recent.size >= 10000 || ++entry.count > 4) return true;
    recent.set(ip, entry);
    return false;
}
export async function complete(env, system, text, maxTokens, upstream, budget) {
    while (true) {
        try {
            const remaining = budget.deadline - Date.now();
            if (remaining <= 0) throw new DOMException("Deadline", "TimeoutError");
            const result = await upstream("https://openrouter.ai/api/v1/chat/completions", {
                method: "POST", signal: AbortSignal.timeout(Math.min(30000, remaining)),
                headers: {"Authorization": "Bearer " + env.OPENROUTER_API_KEY,
                    "Content-Type": "application/json", "X-Title": "PackLight"},
                body: JSON.stringify({
                    model: MODEL, stream: false, max_tokens: maxTokens, reasoning: {enabled: false},
                    messages: [{role: "system", content: system}, {role: "user", content: text}]
                })
            });
            if (!result.ok) {
                await result.body?.cancel();
                throw new Error("provider_" + result.status);
            }
            const data = await result.json();
            const choice = data.choices?.[0];
            const providerError = data.error ?? choice?.error;
            if (providerError) {
                const code = Number(providerError.code);
                throw new Error(Number.isInteger(code) && code >= 400 && code <= 599 ? "provider_" + code : "incomplete");
            }
            const textResult = choice?.message?.content;
            if (choice?.finish_reason !== "stop" || typeof textResult !== "string" || !textResult.trim())
                throw new Error("incomplete");
            return textResult.trim();
        } catch (error) {
            const transient = error?.name === "TimeoutError" || error instanceof TypeError ||
                /^(provider_(408|500|502|503|504)|incomplete)$/.test(error?.message || "");
            if (!transient || budget.retries <= 0 || budget.deadline - Date.now() < 1000) throw error;
            budget.retries--;
            await new Promise(resolve => setTimeout(resolve, 300));
        }
    }
}
export async function handleRequest(request, env, upstream = fetch) {
    const path = new URL(request.url).pathname;
    if (request.method === "GET" && path === "/health")
        return response(200, {service: "PackLight AI", configured: Boolean(env.OPENROUTER_API_KEY)});
    if (request.method !== "POST" || path !== "/chat") return response(404, {safe: false});
    if (!env.OPENROUTER_API_KEY) return response(503, {safe: false});
    try {
        if (await limited(request, env)) return response(429, {safe: false});
        // Bound the body while reading, regardless of the supplied Content-Length.
        const reader = request.body?.getReader();
        if (!reader) return response(400, {safe: false});
        let length = 0;
        const chunks = [];
        while (true) {
            const {done, value} = await reader.read();
            if (done) break;
            length += value.length;
            if (length > 12000) { await reader.cancel(); return response(413, {safe: false}); }
            chunks.push(value);
        }
        const bytes = new Uint8Array(length);
        let offset = 0;
        for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
        let body;
        try { body = JSON.parse(new TextDecoder().decode(bytes)); }
        catch { return response(400, {safe: false}); }
        const {message, destination} = body || {};
        if (typeof message !== "string" || !message.trim() || message.length > 2000 ||
            typeof destination !== "string" || destination.length < 2 || destination.length > 100 ||
            !/^[\p{L}\p{M} .,'’()&-]+$/u.test(destination)) return response(400, {safe: false});
        const budget = {deadline: Date.now() + 75000, retries: 1};
        const reply = await complete(env,
            "You are PackLight, a helpful travel assistant. Answer clearly in under 180 words. " +
            "Do not invent live weather, prices, opening hours or entry rules; state what needs checking. " +
            "Destination: " + destination, message, 1100, upstream, budget);
        if (reply.length > 10000) throw new Error("incomplete");
        // Legacy field retained for older APKs; this response has no app-level content classification.
        return response(200, {safe: true, reply});
    } catch (error) {
        // Return only fixed diagnostic codes, never provider bodies or arbitrary exception messages.
        const code = error?.name === "TimeoutError" ? "timeout"
            : /^(provider_[0-9]{3}|incomplete)$/.test(error?.message || "") ? error.message : "unavailable";
        return response(code === "provider_429" ? 429 : 503, {safe: false, code});
    }
}
export default {fetch(request, env) { return handleRequest(request, env); }};
