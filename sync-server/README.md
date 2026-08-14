# VIVI Music — sync relay (`sync-server/`)

Server WebSocket minimale che permette all'app Android e a **VIVI Music DE (desktop)**
di accoppiarsi **senza account** e di scambiarsi le impostazioni condivise (tema,
lingua, contenuti, qualità audio, testi, integrazioni) e — nelle fasi successive —
coda/posizione di ascolto e libreria.

## Come funziona (spiegazione)

Il telefono e il desktop non possono parlarsi direttamente in modo affidabile:
sono quasi sempre dietro NAT e non hanno un indirizzo raggiungibile. Serve quindi
un **relay**, cioè un punto d'incontro neutro a cui entrambi si collegano in
uscita (WebSocket su `wss://`, quindi cifrato TLS dal provider di hosting).

Il flusso è questo:

```
   Device A (richiedente)          RELAY                  Device B (joiner)
   ----------------------          -----                  ------------------
   pair_request ──────────────────►
                                   genera codice a 6 cifre
   ◄────────────────────────────── pair_code {code}
                                                          pair_join {code} ──►
                                   valida il codice, unisce A e B
   ◄────────────────────────────── pair_joined
                                                          ◄── pair_joined
```

1. **Pairing con codice a scadenza (5 minuti).** Il dispositivo A chiede un codice,
   il relay ne genera uno a 6 cifre e lo tiene in sospeso. Il dispositivo B lo
   inserisce: il relay collega i due `deviceId` in una **coppia** (`pairId`).
   Da quel momento ogni dispositivo conosce l'altro.

2. **Push di snapshot.** Quando A cambia un'impostazione, invia `sync_push` con uno
   snapshot completo. Il relay:
   - lo salva nella **mailbox** di A (per farlo recuperare a B se è offline),
   - se B è online, glielo inoltra subito come `sync`.

3. **Pull (catch-up).** Quando B si connette (o riconnette), invia `sync_pull`:
   il relay risponde con l'ultimo snapshot salvato di A. Così il desktop che era
   spento recupera comunque lo stato del telefono.

4. **Mailbox / persistenza.** Coppie e snapshot vengono salvati best-effort in
   `data.json`, così sopravvivono a un riavvio o allo sleep del free tier Render.
   Un redeploy su free tier azzera il filesystem: in quel caso i dispositivi si
   riaccoppiano semplicemente.

**Sicurezza (fase attuale):** il traffico è protetto **solo da TLS** (terminato dal
provider davanti a questo processo). Il relay **vede** i dati in chiaro. È la scelta
che abbiamo concordato per partire; la cifratura end-to-end arriverà in una fase
successiva, quando il relay non potrà più leggere nulla.

## Eseguire in locale

```bash
cd sync-server
npm install
npm start          # ascolta su ws://localhost:8080
```

Per testare il pairing usa due client (o un tool come `wscat`) collegati a
`ws://localhost:8080`.

## Deploy su Render (consigliato)

1. Crea un nuovo **Web Service** su [render.com](https://render.com).
2. **Repository**: punta al tuo fork di `vivi-music`, branch `vivi-music-de`.
3. **Root Directory**: `sync-server`.
4. **Build Command**: `npm install`.
5. **Start Command**: `npm start`.
6. Scegli il piano **Free** (lo sleep al riavvio è gestito: lo stato è in `data.json`).
7. Prendi l'URL generato (es. `https://xxxx.onrender.com`) e trasformalo in
   `wss://xxxx.onrender.com`: è il valore da impostare nell'app (chiave
   `deviceSyncServerUrl`) e nel desktop.

### Deploy su Hugging Face Spaces

- Crea uno **Space** con Docker SDK, base `node:18`.
- Copia `server.js`, `package.json` dentro lo Space e avvia `node server.js`.
- Hugging Face espone già HTTPS; usa l'URL con `wss://` come sopra.

## Protocollo (riepilogo)

Messaggi JSON, un solo envelope generico:

```jsonc
{ "type": "...", "deviceId": "...", "deviceName": "...",
  "code": "...", "pairId": "...", "snapshot": {...}, "message": "..." }
```

| tipo | direzione | significato |
|---|---|---|
| `pair_request` | C→S | chiede un codice di pairing |
| `pair_code` | S→C | risponde con codice + pairId |
| `pair_join` | C→S | unisce il dispositivo al codice |
| `pair_joined` | S→C | conferma l'avvenuto pairing (a entrambi) |
| `sync_push` | C→S | invia uno snapshot (mailbox + inoltro) |
| `sync_pull` | C→S | chiede l'ultimo snapshot del peer |
| `sync` | S→C | snapshot inoltrato dal peer |
| `no_snapshot` | S→C | nessuno snapshot disponibile |
| `unpair` | C→S | scioglie la coppia |
| `pair_error` | S→C | errore (codice scaduto, non accoppiato, …) |
| `ping` / `pong` | C↔S | keep-alive |
