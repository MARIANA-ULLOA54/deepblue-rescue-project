import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ApiError, request } from '../src/api/client.js'

const mockFetch = (impl) => { globalThis.fetch = impl }

test('devuelve el JSON cuando la respuesta es 2xx', async () => {
  mockFetch(async () => new Response(JSON.stringify([{ caseCode: 'RC-001' }]), { status: 200 }))
  assert.deepEqual(await request('/api/rescue-cases?status=ADMITTED'), [{ caseCode: 'RC-001' }])
})

test('lanza ApiError con status, message y details del ErrorResponse (400)', async () => {
  const body = { status: 400, message: 'Request validation failed', details: { description: 'Description is required' } }
  mockFetch(async () => new Response(JSON.stringify(body), { status: 400 }))
  await assert.rejects(request('/api/treatments', { method: 'POST' }), (e) => {
    assert.ok(e instanceof ApiError)
    assert.equal(e.status, 400)
    assert.equal(e.message, 'Request validation failed')
    assert.deepEqual(e.details, { description: 'Description is required' })
    return true
  })
})

test('lanza ApiError para 404 y 409', async () => {
  for (const status of [404, 409]) {
    mockFetch(async () => new Response(JSON.stringify({ status, message: `m${status}`, details: {} }), { status }))
    await assert.rejects(request('/x'), (e) => e.status === status && e.message === `m${status}`)
  }
})

test('usa mensaje genérico si el cuerpo no es JSON (backend caído)', async () => {
  mockFetch(async () => new Response('<html>Bad gateway</html>', { status: 502 }))
  await assert.rejects(request('/x'), (e) => e.status === 502 && e.message === 'No se pudo contactar el servidor' && Object.keys(e.details).length === 0)
})

test('envía Content-Type JSON y respeta method/body', async () => {
  let captured
  mockFetch(async (url, opts) => { captured = { url, opts }; return new Response('{}', { status: 201 }) })
  await request('/api/treatments', { method: 'POST', body: '{"a":1}' })
  assert.equal(captured.opts.method, 'POST')
  assert.equal(captured.opts.headers['Content-Type'], 'application/json')
})
