/**
 * GetFit Cloud Backend
 * 
 * Features:
 * 1. AI Nutrition Engine Proxy (Google Gemini 2.5 Flash with edge caching)
 * 2. Clean Email/Password Authentication with 6-digit Email OTP Verification
 * 3. Anti-Spam & Disposable Email Domain Filtering (tempmail, 10minutemail, etc.)
 * 4. Silent Cloud Data Sync (Serverless SQLite Database)
 */

export default {
  async fetch(request, env, ctx) {
    // Normalize D1 database binding: supports both GETFIT_DB and DB
    env.DB = env.GETFIT_DB || env.DB;

    // 1. CORS Preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
          "Access-Control-Max-Age": "86400",
        },
      });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    // 2. Health check
    if (path === "/" || path === "/health") {
      return jsonResponse({
        status: "ok",
        service: "GetFit Cloud Backend",
        version: "2.1.0",
        features: ["ai_nutrition", "auth", "otp_verification", "cloud_sync"],
        dbConfigured: Boolean(env.DB)
      });
    }

    // 3. AI Nutrition Search Endpoint
    if (path === "/api/nutrition") {
      return handleNutritionSearch(request, env, ctx, url);
    }

    // 3b. AI Pantry Meal Plan Endpoint (Logged-in users only)
    if (path === "/api/meal-plan" && request.method === "POST") {
      return handleGenerateMealPlan(request, env);
    }

    // 4. Authentication Endpoints
    if (path === "/api/auth/send-otp" && request.method === "POST") {
      return handleSendOtp(request, env);
    }
    if (path === "/api/auth/register" && request.method === "POST") {
      return handleRegister(request, env);
    }
    if (path === "/api/auth/login" && request.method === "POST") {
      return handleLogin(request, env);
    }
    if (path === "/api/auth/reset-password" && request.method === "POST") {
      return handleResetPassword(request, env);
    }
    if ((path === "/api/auth/delete-account" && request.method === "POST") ||
        (path === "/api/auth/account" && request.method === "DELETE")) {
      return handleDeleteAccount(request, env);
    }

    // 5. Cloud Data Sync Endpoints
    if (path === "/api/sync") {
      if (request.method === "POST") {
        return handleSyncUpload(request, env);
      } else if (request.method === "GET") {
        return handleSyncDownload(request, env);
      }
    }

    return jsonResponse({ error: "Endpoint not found" }, 404);
  }
};

/* ========================================================================== */
/*                          ANTI-SPAM / DISPOSABLE EMAILS                     */
/* ========================================================================== */

const DISPOSABLE_EMAIL_DOMAINS = new Set([
  "tempmail.com", "temp-mail.org", "10minutemail.com", "mailinator.com",
  "guerrillamail.com", "guerrillamail.net", "guerrillamail.org", "guerrillamailblock.com",
  "throwawaymail.com", "trashmail.com", "trashmail.net", "trashmail.me",
  "yopmail.com", "yopmail.net", "yopmail.fr", "sharklasers.com", "dispostable.com",
  "getairmail.com", "crazymailing.com", "fakeinbox.com", "generator.email",
  "inboxbear.com", "mohmal.com", "mytemp.email", "tempail.com", "tempm.com",
  "tmpmail.net", "tmpmail.org", "nada.ltd", "dropmail.me", "fakemailgenerator.com",
  "burnermail.io", "maildrop.cc", "emailondeck.com", "trash-mail.com",
  "temp-mail.io", "disposablemail.com", "fakemail.net", "mytempemail.com",
  "tempemailgen.com", "minuteinbox.com", "getnada.com", "inboxkitten.com",
  "throwawaymail.net", "trashmail.io", "byom.de", "mailcatch.com",
  "inbox.si", "disposable.com", "fakeemail.com"
]);

function isAllowedGmail(email) {
  return /^[a-z0-9._+-]{1,64}@gmail\.com$/.test(email) &&
    !email.startsWith(".") &&
    !email.includes("..") &&
    !email.includes(".@");
}

function isDisposableEmail(email) {
  const parts = email.toLowerCase().split("@");
  if (parts.length !== 2) return true;
  const domain = parts[1].trim();
  if (domain !== "gmail.com") return true;
  if (DISPOSABLE_EMAIL_DOMAINS.has(domain)) return true;
  if (
    domain.includes("tempmail") ||
    domain.includes("throwaway") ||
    domain.includes("trashmail") ||
    domain.includes("guerrillamail") ||
    domain.includes("fakeinbox") ||
    domain.includes("10minute")
  ) {
    return true;
  }
  return false;
}

/* ========================================================================== */
/*                          AUTHENTICATION HANDLERS                           */
/* ========================================================================== */

async function handleSendOtp(request, env) {
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  let body;
  try {
    body = await request.json();
  } catch (_) {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const email = (body.email || "").trim().toLowerCase();
  const purpose = body.purpose === "reset" ? "reset" : "register";
  if (!email || !isAllowedGmail(email)) {
    return jsonResponse({ error: "Only @gmail.com email addresses are allowed." }, 400);
  }

  // Check if disposable email
  if (isDisposableEmail(email)) {
    return jsonResponse({
      error: "Temporary or disposable emails are not allowed. Please enter your real email address."
    }, 400);
  }

  const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (purpose === "register" && existing) {
    return jsonResponse({ error: "An account with this email already exists. Please log in." }, 409);
  }
  if (purpose === "reset" && !existing) {
    return jsonResponse({ error: "No account found with this email." }, 404);
  }

  // Anti-Spam: Rate limit by client IP (max 5 OTP emails per 10 minutes per IP) to prevent relay bombing
  const clientIp = request.headers.get("CF-Connecting-IP") || "direct";
  const ipAllowed = await checkRateLimit(env.DB, `otp_ip:${clientIp}`, 5, 10 * 60 * 1000);
  if (!ipAllowed) {
    return jsonResponse({ error: "Too many verification requests from your network. Please wait 10 minutes." }, 429);
  }

  // Rate-limiting: Enforce 60-second cooldown per email + max 3 resends per 15 minutes
  const now = Date.now();
  let existingOtp = null;
  try {
    existingOtp = await env.DB.prepare("SELECT created_at, resend_count FROM email_otps WHERE email = ?").bind(email).first();
  } catch (_) {
    existingOtp = await env.DB.prepare("SELECT created_at FROM email_otps WHERE email = ?").bind(email).first();
  }

  if (existingOtp && (now - existingOtp.created_at) < 60000) {
    const waitSec = Math.ceil((60000 - (now - existingOtp.created_at)) / 1000);
    return jsonResponse({ error: `Please wait ${waitSec}s before requesting another verification code.` }, 429);
  }

  const resendCount = (existingOtp && existingOtp.resend_count !== undefined) ? existingOtp.resend_count : 0;
  if (existingOtp && resendCount >= 3 && (now - existingOtp.created_at) < (15 * 60 * 1000)) {
    return jsonResponse({
      error: "Maximum verification requests reached (3/3). Please wait 15 minutes before requesting a new code."
    }, 429);
  }

  // Generate 6-digit numeric OTP code (expires strictly in 60 seconds)
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = now + (60 * 1000); // 60 seconds
  const newCount = (existingOtp && (now - existingOtp.created_at) < (15 * 60 * 1000)) ? (resendCount + 1) : 1;

  try {
    await env.DB.prepare(`
      INSERT INTO email_otps (email, otp_code, expires_at, created_at, resend_count)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT(email) DO UPDATE SET otp_code = excluded.otp_code, expires_at = excluded.expires_at, created_at = excluded.created_at, resend_count = excluded.resend_count
    `).bind(email, otp, expiresAt, now, newCount).run();
  } catch (_) {
    await env.DB.prepare(`
      INSERT INTO email_otps (email, otp_code, expires_at, created_at)
      VALUES (?, ?, ?, ?)
      ON CONFLICT(email) DO UPDATE SET otp_code = excluded.otp_code, expires_at = excluded.expires_at, created_at = excluded.created_at
    `).bind(email, otp, expiresAt, now).run();
  }

  // Deliver OTP email via Google Mail Relay
  let emailDelivered = false;
  let deliveryError = null;

  if (env.GMAIL_RELAY_URL) {
    if (!env.RELAY_SECRET) {
      return jsonResponse({ error: "Email relay not configured" }, 500);
    }
    const relaySecret = env.RELAY_SECRET;
    try {
      const gasRes = await fetch(env.GMAIL_RELAY_URL, {
        method: "POST",
        redirect: "follow",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          secret: relaySecret,
          to: email,
          subject: "Your GetFit Verification Code: " + otp,
          html: `
            <div style="font-family:sans-serif;max-width:500px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;background:#ffffff;">
              <h2 style="color:#0f172a;margin-top:0;">GetFit Verification Code</h2>
              <p style="color:#475569;font-size:15px;line-height:1.5;">Welcome to GetFit! Enter this code in the app to verify your email and finish creating your account:</p>
              <div style="text-align:center;margin:28px 0;">
                <span style="font-size:34px;letter-spacing:8px;font-weight:bold;color:#2563eb;background:#eff6ff;padding:12px 28px;border-radius:10px;border:1px solid #bfdbfe;display:inline-block;">${otp}</span>
              </div>
              <p style="color:#dc2626;font-size:13px;line-height:1.4;font-weight:bold;">This code expires in 60 seconds. If you did not request this, you can safely ignore this email.</p>
            </div>
          `
        })
      });
      const gasText = await gasRes.text();
      let gasJson = {};
      try { gasJson = JSON.parse(gasText); } catch (_) { }
      if (gasJson.success || gasRes.ok) {
        emailDelivered = true;
      } else {
        deliveryError = "Google Mail Relay error: " + (gasJson.error || gasText);
      }
    } catch (err) {
      deliveryError = "Google Mail Relay failed: " + err.message;
    }
  } else {
    return jsonResponse({
      error: "Email delivery not configured. Please add GMAIL_RELAY_URL secret to your Cloudflare Worker Settings > Variables."
    }, 500);
  }

  if (!emailDelivered) {
    return jsonResponse({
      error: "Failed to send verification email. " + (deliveryError || "Please check your email configuration.")
    }, 502);
  }

  return jsonResponse({
    success: true,
    message: "Verification code sent to " + email
  });
}

async function handleRegister(request, env) {
  if (!env.JWT_SECRET || typeof env.JWT_SECRET !== "string" || env.JWT_SECRET.length < 32) {
    return jsonResponse({ error: "Server not configured" }, 500);
  }
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  let body;
  try {
    body = await request.json();
  } catch (e) {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const email = (body.email || "").trim().toLowerCase();
  const password = body.password || "";
  const name = (body.name || "").trim() || email.split("@")[0] || "User";
  const otp = (body.otp || "").trim();

  if (!email || !isAllowedGmail(email)) {
    return jsonResponse({ error: "Only @gmail.com email addresses are allowed." }, 400);
  }
  if (!password || password.length < 6) {
    return jsonResponse({ error: "Password must be at least 6 characters" }, 400);
  }
  if (!otp || otp.length < 4) {
    return jsonResponse({ error: "6-digit verification code is required" }, 400);
  }

  if (isDisposableEmail(email)) {
    return jsonResponse({ error: "Temporary and disposable emails are not allowed." }, 400);
  }

  // Verify OTP
  const otpRow = await env.DB.prepare(
    "SELECT otp_code, expires_at FROM email_otps WHERE email = ?"
  ).bind(email).first();

  if (!otpRow || otpRow.otp_code !== otp) {
    // Brute-force lockout: after 3 failed guesses, immediately revoke the OTP
    const attemptsAllowed = await checkRateLimit(env.DB, `otp_fail:${email}`, 3, 5 * 60 * 1000);
    if (!attemptsAllowed) {
      await env.DB.prepare("DELETE FROM email_otps WHERE email = ?").bind(email).run();
      return jsonResponse({ error: "Too many incorrect attempts. Verification code revoked for security. Please request a new code." }, 429);
    }
    return jsonResponse({ error: "Invalid verification code. Please check your email or request a new one." }, 400);
  }
  if (Date.now() > otpRow.expires_at) {
    return jsonResponse({ error: "Verification code has expired (60s limit). Please tap 'Resend Code'." }, 400);
  }

  // Check if user already exists
  const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (existing) {
    return jsonResponse({ error: "An account with this email already exists" }, 409);
  }

  // Delete used OTP
  await env.DB.prepare("DELETE FROM email_otps WHERE email = ?").bind(email).run();

  // Hash password with PBKDF2 + SHA-256
  const salt = generateSalt();
  const passwordHash = await hashPassword(password, salt);
  const userId = `usr_${crypto.randomUUID()}`;
  const now = Date.now();

  await env.DB.prepare(
    "INSERT INTO users (id, email, password_hash, salt, name, auth_provider, created_at) VALUES (?, ?, ?, ?, ?, 'email', ?)"
  ).bind(userId, email, passwordHash, salt, name, now).run();

  const token = await signJwt({ sub: userId, email, name }, env.JWT_SECRET);

  return jsonResponse({
    success: true,
    token,
    user: { id: userId, email, name, authProvider: "email" }
  }, 201);
}

async function handleLogin(request, env) {
  if (!env.JWT_SECRET || typeof env.JWT_SECRET !== "string" || env.JWT_SECRET.length < 32) {
    return jsonResponse({ error: "Server not configured" }, 500);
  }
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  let body;
  try {
    body = await request.json();
  } catch (e) {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const email = (body.email || "").trim().toLowerCase();
  const password = body.password || "";

  if (!email || !password) {
    return jsonResponse({ error: "Email and password are required" }, 400);
  }

  // Brute-force protection: Max 10 login attempts per 5 minutes per IP
  const clientIp = request.headers.get("CF-Connecting-IP") || "direct";
  const loginAllowed = await checkRateLimit(env.DB, `login_ip:${clientIp}`, 10, 5 * 60 * 1000);
  if (!loginAllowed) {
    return jsonResponse({ error: "Too many login attempts. Please wait 5 minutes before trying again." }, 429);
  }

  const user = await env.DB.prepare(
    "SELECT id, email, password_hash, salt, name, auth_provider FROM users WHERE email = ?"
  ).bind(email).first();

  if (!user || !user.password_hash || !user.salt) {
    return jsonResponse({ error: "Invalid email or password" }, 401);
  }

  const calculatedHash = await hashPassword(password, user.salt);
  if (calculatedHash !== user.password_hash) {
    return jsonResponse({ error: "Invalid email or password" }, 401);
  }

  const token = await signJwt({ sub: user.id, email: user.email, name: user.name }, env.JWT_SECRET);

  return jsonResponse({
    success: true,
    token,
    user: { id: user.id, email: user.email, name: user.name, authProvider: user.auth_provider }
  });
}

/* ========================================================================== */
/*                            CLOUD SYNC HANDLERS                             */
/* ========================================================================== */

async function handleSyncUpload(request, env) {
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  const authUser = await authenticateRequest(request, env);
  if (!authUser) {
    return jsonResponse({ error: "Unauthorized: Invalid or expired session token" }, 401);
  }

  let body;
  try {
    body = await request.json();
  } catch (e) {
    return jsonResponse({ error: "Invalid JSON payload" }, 400);
  }

  const backupJson = typeof body.backupJson === "string" ? body.backupJson : JSON.stringify(body.backupJson || body);
  if (!backupJson || backupJson === "null" || backupJson === "{}") {
    return jsonResponse({ error: "Empty backup data payload" }, 400);
  }
  // Enforce 5MB limit to prevent database storage exhaustion
  if (backupJson.length > 5 * 1024 * 1024) {
    return jsonResponse({ error: "Backup data exceeds 5MB limit" }, 413);
  }

  const now = Date.now();
  await env.DB.prepare(`
    INSERT INTO user_backups (user_id, backup_json, updated_at)
    VALUES (?, ?, ?)
    ON CONFLICT(user_id) DO UPDATE SET backup_json = excluded.backup_json, updated_at = excluded.updated_at
  `).bind(authUser.sub, backupJson, now).run();

  return jsonResponse({
    success: true,
    message: "Data successfully backed up to cloud",
    syncedAt: now
  });
}

async function handleSyncDownload(request, env) {
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  const authUser = await authenticateRequest(request, env);
  if (!authUser) {
    return jsonResponse({ error: "Unauthorized: Invalid or expired session token" }, 401);
  }

  const row = await env.DB.prepare(
    "SELECT backup_json, updated_at FROM user_backups WHERE user_id = ?"
  ).bind(authUser.sub).first();

  if (!row) {
    return jsonResponse({ backupJson: null, updatedAt: null, message: "No cloud backup found for this account" });
  }

  return jsonResponse({
    success: true,
    backupJson: row.backup_json,
    updatedAt: row.updated_at
  });
}

/* ========================================================================== */
/*                       AI NUTRITION SEARCH PROXY                            */
/* ========================================================================== */

async function handleNutritionSearch(request, env, ctx, url) {
  const authUser = await authenticateRequest(request, env);
  if (!authUser) {
    return jsonResponse({ error: "Unauthorized: Please log in to use AI nutrition search" }, 401);
  }

  let query = url.searchParams.get("q") || url.searchParams.get("query");
  if (!query && request.method === "POST") {
    try {
      const body = await request.json();
      query = body.query || body.q;
    } catch (_) { }
  }

  const trimmed = (query || "").trim();
  if (!trimmed) {
    return jsonResponse({ error: "Missing 'q' or 'query' parameter" }, 400);
  }
  if (trimmed.length > 100) {
    return jsonResponse({ error: "Search query exceeds 100 characters limit" }, 400);
  }

  // Rate limit Gemini API proxy per IP to prevent quota draining (max 30/min)
  const clientIp = request.headers.get("CF-Connecting-IP") || "direct";
  if (env.DB) {
    const allowed = await checkRateLimit(env.DB, `nutrition_ip:${clientIp}`, 30, 60 * 1000);
    if (!allowed) {
      return jsonResponse({ error: "Rate limit exceeded. Please wait a minute before making more requests." }, 429);
    }
  }

  const apiKey = env.GEMINI_API_KEY;
  if (!apiKey) {
    return jsonResponse({ error: "GEMINI_API_KEY environment secret is not configured" }, 500);
  }

  // Check Cloudflare Edge Cache
  const cacheKey = new Request(url.toString(), request);
  const cache = caches.default;
  const cachedResponse = await cache.match(cacheKey);
  if (cachedResponse) {
    return cachedResponse;
  }

  const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;
  const promptText = `Act as an expert nutritional scientist and calculate accurate nutritional macros per single serving or 100g for: "${trimmed}".
Return ONLY a valid JSON array of up to 4 food items matching this exact schema, with no markdown code blocks or extra text:
[
  {
    "name": "Food Name with serving clarity",
    "category": "Indian Breads / Non-Veg / Dairy / South Indian / etc.",
    "caloriesPer100g": 250.0,
    "proteinPer100g": 12.0,
    "carbsPer100g": 25.0,
    "fatPer100g": 10.0,
    "fiberPer100g": 2.0,
    "defaultServingSize": 1.0,
    "servingUnit": "piece",
    "source": "AI Verified"
  }
]
If the query is nonsensical, random letters, gibberish, or not a food or beverage (e.g. 'dshhd', 'laptop', 'asdf'), return an empty JSON array [].
Calculate macros realistically taking into account standard ingredients, cooking oils, and preparations. Ensure calories approximately equal protein*4 + carbs*4 + fat*9.`;

  const requestPayload = {
    contents: [{ parts: [{ text: promptText }] }],
    generationConfig: {
      temperature: 0.1,
      responseMimeType: "application/json"
    }
  };

  try {
    const response = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestPayload)
    });

    if (!response.ok) {
      const errText = await response.text();
      return jsonResponse({ error: "Gemini API error", status: response.status, details: errText }, response.status);
    }

    const data = await response.json();
    const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "[]";
    let parsed = [];
    try {
      parsed = JSON.parse(rawText.trim());
    } catch (_) {
      parsed = [];
    }

    const clientResponse = new Response(JSON.stringify(parsed), {
      status: 200,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Cache-Control": "public, max-age=86400, s-maxage=86400"
      }
    });

    if (ctx && typeof ctx.waitUntil === "function") {
      ctx.waitUntil(cache.put(cacheKey, clientResponse.clone()));
    }
    return clientResponse;
  } catch (err) {
    return jsonResponse({ error: "Proxy connection error", details: err.message }, 502);
  }
}

/* ========================================================================== */
/*                             HELPER UTILITIES                               */
/* ========================================================================== */

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*"
    }
  });
}

function dbMissingResponse() {
  return jsonResponse({
    error: "Database binding 'GETFIT_DB' is missing in worker settings.",
    resolution: "In Cloudflare dashboard, go to Workers & Pages -> getfit-nutrition-proxy -> Settings -> Bindings -> Add binding -> D1 database. Set variable name to 'GETFIT_DB' and pick 'getfit-db'."
  }, 503);
}

let dbReady = false;
async function ensureDbTables(db) {
  if (dbReady) return;
  try {
    await db.batch([
      db.prepare(`
        CREATE TABLE IF NOT EXISTS users (
          id TEXT PRIMARY KEY,
          email TEXT UNIQUE NOT NULL,
          password_hash TEXT,
          salt TEXT,
          name TEXT,
          auth_provider TEXT DEFAULT 'email',
          created_at INTEGER
        );
      `),
      db.prepare(`
        CREATE TABLE IF NOT EXISTS user_backups (
          user_id TEXT PRIMARY KEY,
          backup_json TEXT NOT NULL,
          updated_at INTEGER
        );
      `),
      db.prepare(`
        CREATE TABLE IF NOT EXISTS email_otps (
          email TEXT PRIMARY KEY,
          otp_code TEXT NOT NULL,
          expires_at INTEGER NOT NULL,
          created_at INTEGER NOT NULL
        );
      `),
      db.prepare(`
        CREATE TABLE IF NOT EXISTS rate_limits (
          key TEXT PRIMARY KEY,
          count INTEGER NOT NULL,
          reset_at INTEGER NOT NULL
        );
      `)
    ]);
    dbReady = true;
  } catch (e) {
    // If batch fails, allow individual queries
  }
}

async function checkRateLimit(db, key, maxRequests, windowMs) {
  if (!db) return true;
  const now = Date.now();
  try {
    const row = await db.prepare("SELECT count, reset_at FROM rate_limits WHERE key = ?").bind(key).first();
    if (!row || now > row.reset_at) {
      await db.prepare("INSERT OR REPLACE INTO rate_limits (key, count, reset_at) VALUES (?, 1, ?)").bind(key, now + windowMs).run();
      return true;
    }
    if (row.count >= maxRequests) {
      return false;
    }
    await db.prepare("UPDATE rate_limits SET count = count + 1 WHERE key = ?").bind(key).run();
    return true;
  } catch (_) {
    return true; // Gracefully allow if table is busy
  }
}

async function hashPassword(password, saltHex) {
  const encoder = new TextEncoder();
  const salt = new Uint8Array(saltHex.match(/.{1,2}/g).map(byte => parseInt(byte, 16)));
  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    encoder.encode(password),
    { name: "PBKDF2" },
    false,
    ["deriveBits"]
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      salt: salt,
      iterations: 100000,
      hash: "SHA-256"
    },
    keyMaterial,
    256
  );
  return Array.from(new Uint8Array(derivedBits))
    .map(b => b.toString(16).padStart(2, "0"))
    .join("");
}

function generateSalt() {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes).map(b => b.toString(16).padStart(2, "0")).join("");
}

async function getJwtKey(secret) {
  if (!secret || typeof secret !== "string" || secret.length < 32) {
    return null;
  }
  return await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

function base64UrlEncode(str) {
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function base64UrlDecode(str) {
  str = str.replace(/-/g, "+").replace(/_/g, "/");
  while (str.length % 4) str += "=";
  return atob(str);
}

async function signJwt(payload, secret) {
  const header = { alg: "HS256", typ: "JWT" };
  const headerB64 = base64UrlEncode(JSON.stringify(header));
  const fullPayload = {
    ...payload,
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + (365 * 86400) // 1 year session
  };
  const payloadB64 = base64UrlEncode(JSON.stringify(fullPayload));
  const data = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
  const key = await getJwtKey(secret);
  if (!key) throw new Error("Server not configured");
  const signature = await crypto.subtle.sign("HMAC", key, data);
  const sigB64 = base64UrlEncode(String.fromCharCode(...new Uint8Array(signature)));
  return `${headerB64}.${payloadB64}.${sigB64}`;
}

async function verifyJwt(token, secret) {
  try {
    const key = await getJwtKey(secret);
    if (!key) return null;
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const [headerB64, payloadB64, sigB64] = parts;
    const data = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
    const sigStr = base64UrlDecode(sigB64);
    const sigBytes = new Uint8Array(sigStr.length);
    for (let i = 0; i < sigStr.length; i++) sigBytes[i] = sigStr.charCodeAt(i);
    const valid = await crypto.subtle.verify("HMAC", key, sigBytes, data);
    if (!valid) return null;
    const payload = JSON.parse(base64UrlDecode(payloadB64));
    if (payload.exp && Date.now() / 1000 > payload.exp) return null;
    return payload;
  } catch (_) {
    return null;
  }
}

async function authenticateRequest(request, env) {
  const authHeader = request.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) return null;
  const token = authHeader.substring(7).trim();
  if (!token) return null;
  return await verifyJwt(token, env.JWT_SECRET);
}

/* ========================================================================== */
/*                       AI PANTRY MEAL PLAN GENERATOR                        */
/* ========================================================================== */

async function handleGenerateMealPlan(request, env) {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return jsonResponse({ error: "Please sign in to use AI Meal Plans." }, 401);
  }

  if (env.DB) {
    const allowed = await checkRateLimit(env.DB, `mealplan_user:${user.sub}`, 10, 60 * 1000);
    if (!allowed) {
      return jsonResponse({ error: "Too many meal plan requests. Please wait a minute." }, 429);
    }
  }

  let body;
  try {
    body = await request.json();
  } catch (_) {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const items = String(body.items || "").trim();
  if (!items || items.length < 3) {
    return jsonResponse({ error: "Please enter the ingredients you have at home." }, 400);
  }
  if (items.length > 600) {
    return jsonResponse({ error: "Ingredients list exceeds 600 characters limit." }, 400);
  }

  const targetCalories = Math.max(1000, Math.min(6000, Number(body.targetCalories) || 2200));
  const targetProtein = Math.max(30, Math.min(400, Number(body.targetProtein) || 130));
  const targetCarbs = Math.max(30, Math.min(700, Number(body.targetCarbs) || 250));
  const targetFat = Math.max(20, Math.min(250, Number(body.targetFat) || 65));
  const goal = String(body.goal || "Balanced Fitness").slice(0, 60);

  const apiKey = env.GEMINI_API_KEY;
  if (!apiKey) {
    return jsonResponse({ error: "GEMINI_API_KEY environment secret is not configured" }, 500);
  }

  const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;
  const promptText = `Act as an expert sports nutritionist. The user has these ingredients at home:
"${items}"

User's Profile Goal: ${goal}
Daily Targets: ~${targetCalories} kcal, ~${targetProtein}g protein, ~${targetCarbs}g carbs, ~${targetFat}g fat.

Create a complete, practical 1-day meal plan (Breakfast, Lunch, Dinner, Snacks) using primarily the inputted ingredients (you may assume basic kitchen staples like water, salt, spices, and minimal cooking oil).
Rules:
1. Tailor macro distribution and portion sizes to support the user's "${goal}" goal while hitting the daily targets (~${targetCalories} kcal, ~${targetProtein}g protein, ~${targetCarbs}g carbs, ~${targetFat}g fat) across the 4 meals.
2. Express ALL ingredient weights and portions in metric grams (g) or milliliters (ml) — e.g. "Chicken breast (180g)", "Rolled oats (60g)", "Whole eggs (2 eggs / 100g)", "Milk (250ml)". NEVER use ounces (oz), pounds (lb), or cups.

Return ONLY valid JSON matching this exact schema (no markdown or extra text):
{
  "summary": "Short 1-sentence overview of how the macros support the ${goal} goal",
  "totalCalories": 2200,
  "totalProtein": 150,
  "totalCarbs": 230,
  "totalFat": 65,
  "meals": [
    {
      "mealType": "Breakfast",
      "title": "Short Meal Title",
      "calories": 550,
      "protein": 38,
      "carbs": 58,
      "fat": 16,
      "items": ["Rolled oats (60g)", "Whole eggs (2 eggs / 100g)"]
    }
  ]
}
Ensure "meals" has exactly 4 entries in order: Breakfast, Lunch, Dinner, Snacks.`;

  try {
    const response = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: promptText }] }],
        generationConfig: {
          temperature: 0.3,
          responseMimeType: "application/json",
          thinkingConfig: { thinkingBudget: 0 }
        }
      })
    });

    if (!response.ok) {
      const errText = await response.text();
      return jsonResponse({ error: "Gemini API error", status: response.status, details: errText }, response.status);
    }

    const data = await response.json();
    const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "{}";
    let parsed;
    try {
      parsed = JSON.parse(rawText.trim());
    } catch (_) {
      return jsonResponse({ error: "Failed to parse AI meal plan response." }, 502);
    }

    return jsonResponse(parsed, 200);
  } catch (err) {
    return jsonResponse({ error: "Meal plan generation failed", details: err.message }, 500);
  }
}

/* ========================================================================== */
/*                              DELETE ACCOUNT                                */
/* ========================================================================== */

async function handleResetPassword(request, env) {
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  let body;
  try {
    body = await request.json();
  } catch (_) {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const email = (body.email || "").trim().toLowerCase();
  const otp = (body.otp || "").trim();
  const newPassword = body.newPassword || "";

  if (!email || !isAllowedGmail(email)) {
    return jsonResponse({ error: "Only @gmail.com email addresses are allowed." }, 400);
  }
  if (!otp || otp.length < 4) {
    return jsonResponse({ error: "6-digit verification code is required." }, 400);
  }
  if (!newPassword || newPassword.length < 6) {
    return jsonResponse({ error: "Password must be at least 6 characters." }, 400);
  }

  const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
  if (!existing) {
    return jsonResponse({ error: "No account found with this email." }, 404);
  }

  const otpRow = await env.DB.prepare(
    "SELECT otp_code, expires_at FROM email_otps WHERE email = ?"
  ).bind(email).first();

  if (!otpRow || otpRow.otp_code !== otp) {
    const attemptsAllowed = await checkRateLimit(env.DB, `otp_fail:${email}`, 3, 5 * 60 * 1000);
    if (!attemptsAllowed) {
      await env.DB.prepare("DELETE FROM email_otps WHERE email = ?").bind(email).run();
      return jsonResponse({ error: "Too many incorrect attempts. Verification code revoked. Please request a new code." }, 429);
    }
    return jsonResponse({ error: "Invalid verification code. Please check your email or request a new one." }, 400);
  }
  if (Date.now() > otpRow.expires_at) {
    return jsonResponse({ error: "Verification code has expired (60s limit). Please tap 'Resend Code'." }, 400);
  }

  await env.DB.prepare("DELETE FROM email_otps WHERE email = ?").bind(email).run();

  const salt = generateSalt();
  const passwordHash = await hashPassword(newPassword, salt);
  await env.DB.prepare(
    "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?"
  ).bind(passwordHash, salt, existing.id).run();

  return jsonResponse({
    success: true,
    message: "Password reset successfully. You can now log in."
  });
}

async function handleDeleteAccount(request, env) {
  const user = await authenticateRequest(request, env);
  if (!user) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }
  if (!env.DB) return dbMissingResponse();
  await ensureDbTables(env.DB);

  let body = {};
  try {
    body = await request.json();
  } catch (_) {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const confirmEmail = String(body.email || "").trim().toLowerCase();
  const dbUser = await env.DB.prepare("SELECT id, email FROM users WHERE id = ?").bind(user.sub).first();
  if (!dbUser) {
    return jsonResponse({ error: "Account not found" }, 404);
  }

  if (!confirmEmail || confirmEmail !== String(dbUser.email || "").trim().toLowerCase()) {
    return jsonResponse({ error: "Entered email does not match your account email." }, 400);
  }

  await env.DB.batch([
    env.DB.prepare("DELETE FROM user_backups WHERE user_id = ?").bind(dbUser.id),
    env.DB.prepare("DELETE FROM email_otps WHERE email = ?").bind(dbUser.email),
    env.DB.prepare("DELETE FROM rate_limits WHERE key = ? OR key = ?").bind(`mealplan_user:${dbUser.id}`, `otp_fail:${dbUser.email}`),
    env.DB.prepare("DELETE FROM users WHERE id = ?").bind(dbUser.id)
  ]);

  return jsonResponse({
    success: true,
    message: "Account and all cloud data permanently deleted."
  });
}
