import express from 'express'
import makeWASocket, {
  DisconnectReason,
  fetchLatestBaileysVersion,
  useMultiFileAuthState
} from '@whiskeysockets/baileys'
import pino from 'pino'
import qrcode from 'qrcode-terminal'
import { Boom } from '@hapi/boom'
import fs from 'node:fs/promises'
import path from 'node:path'

const PORT = Number(process.env.PORT || 8787)
const AUTH_DIR = path.resolve(process.env.WA_AUTH_DIR || './auth')
const logger = pino({ level: process.env.LOG_LEVEL || 'info' })
const app = express()

app.use(express.json({ limit: '2mb' }))

let sock = null
let saveCreds = null
let state = { status: 'disconnected', qrData: null, phoneNumber: null, error: null }
let seq = 0
const messages = []

function publicState() {
  return { ...state }
}

function normalizePhone(phoneNumber) {
  return String(phoneNumber || '').replace(/\D/g, '')
}

async function startSocket() {
  if (sock) return publicState()

  state = { status: 'connecting', qrData: null, phoneNumber: null, error: null }
  const auth = await useMultiFileAuthState(AUTH_DIR)
  saveCreds = auth.saveCreds
  const { version } = await fetchLatestBaileysVersion()

  sock = makeWASocket({
    version,
    auth: auth.state,
    printQRInTerminal: false,
    browser: ['ZapBot Android', 'Chrome', '1.0.0'],
    logger: pino({ level: 'silent' })
  })

  sock.ev.on('creds.update', saveCreds)

  sock.ev.on('connection.update', (update) => {
    if (update.qr) {
      state = { status: 'waiting_qr', qrData: update.qr, phoneNumber: null, error: null }
      qrcode.generate(update.qr, { small: true })
      logger.info('QR atualizado')
    }

    if (update.connection === 'open') {
      state = {
        status: 'running',
        qrData: null,
        phoneNumber: sock?.user?.id?.split(':')[0] || null,
        error: null
      }
      logger.info({ phoneNumber: state.phoneNumber }, 'WhatsApp conectado')
    }

    if (update.connection === 'close') {
      const code = new Boom(update.lastDisconnect?.error).output?.statusCode
      sock = null
      if (code === DisconnectReason.loggedOut) {
        state = { status: 'disconnected', qrData: null, phoneNumber: null, error: 'Sessao encerrada no WhatsApp' }
      } else {
        state = { status: 'error', qrData: null, phoneNumber: null, error: 'Conexao fechada. Inicie novamente.' }
      }
      logger.warn({ code }, 'WhatsApp desconectado')
    }
  })

  sock.ev.on('messages.upsert', ({ messages: incoming }) => {
    for (const msg of incoming || []) {
      if (!msg.message || msg.key.fromMe) continue
      const text =
        msg.message.conversation ||
        msg.message.extendedTextMessage?.text ||
        msg.message.imageMessage?.caption ||
        msg.message.videoMessage?.caption ||
        ''
      if (!text) continue
      messages.push({
        seq: ++seq,
        id: msg.key.id || String(seq),
        chatId: msg.key.remoteJid,
        senderName: msg.pushName || null,
        text,
        timestamp: Number(msg.messageTimestamp || Math.floor(Date.now() / 1000)) * 1000
      })
      if (messages.length > 500) messages.shift()
    }
  })

  return publicState()
}

app.get('/health', (_req, res) => res.json({ ok: true }))

app.get('/state', (_req, res) => res.json(publicState()))

app.post('/start', async (_req, res) => {
  try {
    res.json(await startSocket())
  } catch (error) {
    state = { status: 'error', qrData: null, phoneNumber: null, error: error.message }
    logger.error({ error }, 'Falha ao iniciar socket')
    res.status(500).json(publicState())
  }
})

app.post('/pairing-code', async (req, res) => {
  try {
    await startSocket()
    const phoneNumber = normalizePhone(req.body?.phoneNumber)
    if (phoneNumber.length < 10) return res.status(400).json({ error: 'Numero invalido' })
    if (!sock) return res.status(409).json({ error: 'Socket nao iniciado' })
    if (sock.authState.creds.registered) {
      return res.json({ code: 'JA-CONECTADO', state: publicState() })
    }
    const code = await sock.requestPairingCode(phoneNumber)
    state = { ...state, status: 'waiting_qr', error: null }
    logger.info({ phoneNumber }, 'Codigo de pareamento gerado')
    res.json({ code, state: publicState() })
  } catch (error) {
    state = { status: 'error', qrData: null, phoneNumber: null, error: error.message }
    logger.error({ error }, 'Falha ao gerar codigo')
    res.status(500).json({ error: error.message, state: publicState() })
  }
})

app.post('/send-text', async (req, res) => {
  try {
    if (!sock || state.status !== 'running') return res.status(409).json({ error: 'WhatsApp nao conectado' })
    const { chatId, text } = req.body || {}
    if (!chatId || !text) return res.status(400).json({ error: 'chatId/text obrigatorios' })
    await sock.sendMessage(chatId, { text })
    res.json({ ok: true })
  } catch (error) {
    logger.error({ error }, 'Falha ao enviar texto')
    res.status(500).json({ error: error.message })
  }
})

app.get('/messages', (req, res) => {
  const after = Number(req.query.after || 0)
  res.json({ messages: messages.filter((message) => message.seq > after) })
})

app.post('/stop', async (_req, res) => {
  if (sock) {
    try { await sock.end(undefined) } catch {}
  }
  sock = null
  state = { status: 'disconnected', qrData: null, phoneNumber: null, error: null }
  res.json(publicState())
})

app.post('/clear-session', async (_req, res) => {
  if (sock) {
    try { await sock.logout() } catch {}
    try { await sock.end(undefined) } catch {}
  }
  sock = null
  await fs.rm(AUTH_DIR, { recursive: true, force: true })
  state = { status: 'disconnected', qrData: null, phoneNumber: null, error: null }
  seq = 0
  messages.splice(0, messages.length)
  res.json(publicState())
})

app.listen(PORT, '0.0.0.0', () => {
  logger.info(`ZapBot WhatsApp bridge em http://0.0.0.0:${PORT}`)
})
