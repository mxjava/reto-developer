const encoder = new TextEncoder();

function fromBase64(value) {
  const binary = atob(value);
  return Uint8Array.from(binary, character => character.charCodeAt(0));
}

function toBase64(bytes) {
  let binary = '';
  bytes.forEach(byte => { binary += String.fromCharCode(byte); });
  return btoa(binary);
}

export async function encryptSecret(plainText) {
  const encodedSecret = import.meta.env.VITE_AES_KEY_BASE64;
  let keyBytes;
  try {
    keyBytes = fromBase64(encodedSecret ?? '');
  } catch {
    throw new Error('VITE_AES_KEY_BASE64 no contiene Base64 valido');
  }
  if (keyBytes.length !== 32) {
    throw new Error('VITE_AES_KEY_BASE64 debe representar exactamente 32 bytes');
  }

  const key = await crypto.subtle.importKey(
    'raw', keyBytes, { name: 'AES-GCM' }, false, ['encrypt']);

  // Cada operación usa un IV nuevo de 96 bits; nunca se reutiliza un IV con la misma llave GCM.
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encrypted = new Uint8Array(await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv, tagLength: 128 }, key, encoder.encode(plainText)));

  // API 1 espera el formato Base64(IV || ciphertext || tag GCM).
  const payload = new Uint8Array(iv.length + encrypted.length);
  payload.set(iv, 0);
  payload.set(encrypted, iv.length);
  return toBase64(payload);
}
