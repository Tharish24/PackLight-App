# Optional AI gateway
Run with Node.js 22+ (no npm dependencies):
1. Set GEMINI_API_KEY in the server environment using your hosting provider's secret store.
2. Set GEMINI_MODEL to a currently supported stable Gemini model with generateContent and JSON output support.
3. Run node server.mjs.
4. Put an HTTPS reverse proxy in front of loopback port 8080, with request rate limits, usage quotas and access controls appropriate to your distribution.
5. Set PACKLIGHT_AI_URL to https://your-host/chat when building the Android app.

The server independently classifies both input and output, applies strict provider safety settings, checks offensive language, and fails closed.
It processes one question at a time without storing conversation history. Provider data handling follows your Gemini account terms.
The example has a four-request concurrency limit; configure per-client limits at the gateway before public deployment.
No provider calls were tested here: credentials and Node.js are absent from this workstation. Android build checks do not validate this optional server.

References:
- https://ai.google.dev/api/generate-content
- https://ai.google.dev/gemini-api/docs/safety-settings

All automated filters can miss context or obfuscation. Validate the deployed model against a representative adversarial safety set before release.
