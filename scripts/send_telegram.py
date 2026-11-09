import os
import sys
import urllib.request
import uuid

def send_telegram_apk():
    bot_token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID")
    thread_id = os.environ.get("TELEGRAM_THREAD_ID", "").strip()
    apk_path = os.environ.get("APK_PATH")
    commit_sha = os.environ.get("COMMIT_SHA", "")
    apk_size = os.environ.get("APK_SIZE", "")

    if not bot_token or not chat_id or not apk_path:
        print("Error: Missing required environment variables (TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID, or APK_PATH)")
        sys.exit(1)

    if not os.path.isfile(apk_path):
        print(f"Error: APK file not found at path: {apk_path}")
        sys.exit(1)

    short_sha = commit_sha[:7] if commit_sha else "latest"
    commit_url = f"https://github.com/vivizzz007/vivi-music/commit/{commit_sha}" if commit_sha else "#"

    caption = (
        f"🎧 <b>ViviMusic Nightly Build</b>\n"
        f"━━━━━━━━━━━━━━━━━━━━\n"
        f"📱 <b>Variant:</b> Universal GMS Release\n"
        f"🌿 <b>Branch:</b> <code>beta</code>\n"
        f"📦 <b>File Size:</b> {apk_size}\n"
        f"🔗 <b>Commit:</b> <a href=\"{commit_url}\">{short_sha}</a>\n"
        f"━━━━━━━━━━━━━━━━━━━━\n"
        f"🚀 <i>Compiled automatically with the latest updates!</i>"
    )

    boundary = f"------------------------{uuid.uuid4().hex}"
    
    parts = []

    def add_field(name, value):
        parts.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n{value}\r\n".encode("utf-8"))

    add_field("chat_id", chat_id)
    if thread_id:
        add_field("message_thread_id", thread_id)
    add_field("parse_mode", "HTML")
    add_field("caption", caption)

    filename = os.path.basename(apk_path)
    print(f"Reading APK file: {apk_path} ({os.path.getsize(apk_path)} bytes)...")
    with open(apk_path, "rb") as f:
        file_data = f.read()

    file_header = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="document"; filename="{filename}"\r\n'
        f"Content-Type: application/vnd.android.package-archive\r\n\r\n"
    ).encode("utf-8")

    parts.append(file_header + file_data + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode("utf-8"))

    payload = b"".join(parts)

    url = f"https://api.telegram.org/bot{bot_token}/sendDocument"
    req = urllib.request.Request(
        url,
        data=payload,
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Content-Length": str(len(payload))
        },
        method="POST"
    )

    print(f"Uploading to Telegram (chat_id: {chat_id}, thread: {thread_id})...")
    
    try:
        with urllib.request.urlopen(req, timeout=300) as response:
            res_body = response.read().decode("utf-8")
            print("Telegram Response SUCCESS:")
            print(res_body)
    except Exception as e:
        print(f"Error sending file to Telegram: {e}")
        if hasattr(e, "read"):
            try:
                print("Telegram Error Details:", e.read().decode("utf-8"))
            except Exception:
                pass
        sys.exit(1)

if __name__ == "__main__":
    send_telegram_apk()
