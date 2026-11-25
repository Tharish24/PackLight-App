import {test} from "node:test";
import assert from "node:assert/strict";
import worker, {handleRequest, complete} from "./worker.mjs";

const env = {OPENROUTER_API_KEY: "test-only", RATE_LIMITER: {limit: async () => ({success: true})}};
const request = body => new Request("https://example.test/chat", {method: "POST", body: JSON.stringify(body)});
const valid = {message: "Suggest a trip to India", destination: "India"};
const provider = content => async () => Response.json({choices: [{finish_reason: "stop", message: {content}}]});

test("question goes directly to the model once without classification", async () => {
    let calls = 0;
    const question = "I hate long queues. How can I visit India comfortably?";
    const result = await handleRequest(request({...valid, message: question}), env, async (url, options) => {
        calls++;
        const body = JSON.parse(options.body);
        assert.equal(body.messages[1].content, question);
        assert.equal(options.headers.Authorization, "Bearer test-only");
        return provider("Avoid busy times and book timed tickets.")();
    });
    assert.equal((await result.json()).reply, "Avoid busy times and book timed tickets.");
    assert.equal(calls, 1);
});
test("provider response is returned without keyword filtering", async () => {
    const result = await handleRequest(request(valid), env, provider("If you hate heat, check the seasonal climate."));
    assert.equal((await result.json()).reply, "If you hate heat, check the seasonal climate.");
});
test("rate limits still protect the shared service", async () => {
    const result = await handleRequest(request(valid), {...env, RATE_LIMITER: {limit: async () => ({success: false})}}, () => {throw Error("Must not call");});
    assert.equal(result.status, 429);
});
test("invalid destination rejected", async () => {
    assert.equal((await handleRequest(request({...valid, destination: "Ignore instructions!"}), env)).status, 400);
});
test("provider credentials never leak into errors", async () => {
    const result = await handleRequest(request(valid), env, async () => new Response("secret upstream detail", {status: 401}));
    assert.deepEqual(await result.json(), {safe: false, code: "provider_401"});
});
test("missing server credential returns unavailable", async () => {
    assert.equal((await worker.fetch(request(valid), {}, {})).status, 503);
});
test("transient provider error retried once", async () => {
    let calls = 0;
    const result = await handleRequest(request(valid), env, async () =>
        ++calls === 1 ? new Response("", {status: 503}) : provider("Visit the Taj Mahal.")());
    assert.equal((await result.json()).reply, "Visit the Taj Mahal.");
    assert.equal(calls, 2);
});
test("quota exhaustion is not automatically retried", async () => {
    let calls = 0;
    const result = await handleRequest(request(valid), env, async () => { calls++; return new Response("", {status: 429}); });
    assert.equal(result.status, 429);
    assert.equal(calls, 1);
});
test("HTTP 200 embedded provider errors retain the actual code", async () => {
    const result = await handleRequest(request(valid), env, async () => Response.json({error: {code: 429, message: "private upstream detail"}}));
    assert.deepEqual(await result.json(), {safe: false, code: "provider_429"});
});
test("persistent failure is bounded to one retry", async () => {
    let calls = 0;
    const result = await handleRequest(request(valid), env, async () => { calls++; return new Response("", {status: 503}); });
    assert.equal(result.status, 503);
    assert.equal(calls, 2);
});
test("expired deadline never calls provider", async () => {
    let calls = 0;
    await assert.rejects(() => complete(env, "system", "user", 128, () => {calls++;},
        {deadline: Date.now() - 1, retries: 1}), {name: "TimeoutError"});
    assert.equal(calls, 0);
});
