# PackLight app-owned AI: Cloudflare Workers

Public users never enter API keys. Android sends a question and destination to your HTTPS Worker; the Worker authenticates to OpenRouter with your server secret and returns the model answer directly.

One-time owner deployment:
1. Install the current Node.js LTS from https://nodejs.org/en/download and reopen your terminal.
2. In this backend directory run: npx wrangler@4 login
3. Run: npx wrangler@4 deploy
4. Run: npx wrangler@4 secret put OPENROUTER_API_KEY
   Paste your OpenRouter key into Wrangler's hidden prompt, not into a project file.
5. Open https://packlight-ai.YOUR-SUBDOMAIN.workers.dev/health and confirm configured is true.
6. Set PACKLIGHT_AI_URL=https://packlight-ai.YOUR-SUBDOMAIN.workers.dev/chat in the Android project's Gradle properties.
7. Rebuild and reinstall the Android app. New installations work without a key prompt.

Cloudflare dashboard alternative:
Create a Worker named packlight-ai, paste worker.mjs into its code editor, and deploy.
In Settings → Variables and Secrets add OPENROUTER_API_KEY as a Secret.
Dashboard-only deployment uses an in-memory per-isolate rate limiter; Wrangler deployment uses the configured Cloudflare rate-limit binding.
Send your actual Worker URL to the app developer to complete the Android configuration.

No provider key is embedded in the APK. The old personal key screen is retired and old device credentials are removed on launch.
The Worker uses nvidia/nemotron-3.5-super-vl:free. One provider call per chat generates the answer. The custom keyword blocklist and model-based moderation passes have been removed. A single shared retry is allowed for timeout, incomplete or transient server failures; the entire request has a 75-second deadline. Quota, account and safety failures are not automatically retried.
The free provider quota is shared by all app users. Responses can be unavailable when this quota is exhausted.
PackLight does not apply its own content filters. OpenRouter and model providers may still impose their own restrictions.
NVIDIA's free endpoint logs requests. The app must disclose provider processing and users should not send personal/confidential data.

Rate limits are approximate and per Cloudflare location (4 requests/minute per client IP). They are not authentication or a global budget cap.
Set provider spending/usage limits and add verified app attestation or user authentication before a large public rollout.
Cloudflare Worker logs are disabled in wrangler.jsonc; no keys, questions or upstream error bodies are logged in application code.
For local tests, Node 22+ supports: node --test worker.test.mjs (mock provider; no real key needed).

The previous server.mjs is a legacy Gemini example; worker.mjs is the public OpenRouter deployment entry point.
References:
https://developers.cloudflare.com/workers/configuration/secrets/
https://developers.cloudflare.com/workers/runtime-apis/bindings/rate-limit/
https://openrouter.ai/docs/api/reference/overview
