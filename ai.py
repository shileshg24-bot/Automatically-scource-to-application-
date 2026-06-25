import os
import zipfile
import asyncio
import json
import shutil
import google.generativeai as genai
from telegram import Update
from telegram.ext import ApplicationBuilder, MessageHandler, CommandHandler, filters

# ================================
# CONFIG - YAHAN APNI KEYS DAALO
# ================================
BOT_TOKEN = "8178770651:AAFJJp4TutKVkO74av7LzwT9gd47nG7-6gM"
GEMINI_API_KEY = "AQ.Ab8RN6Lm6LD0M_ewZaxLRLRMaoZ7NAKYvSyCoGJcCOQrvmp2Pw"

genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel('gemini-pro')

BUILD_DIR = "workspace"
active_process = None

# ================================
# /start
# ================================
async def start(update, context):
    await update.message.reply_text(
        "🤖 Android Build Bot\n\n"
        "📌 Commands:\n"
        "/codespace - Libraries install karo\n"
        "/stop - Process rok do\n\n"
        "📦 ZIP bhejo APK banane ke liye!"
    )

# ================================
# /codespace
# ================================
async def setup_codespace(update, context):
    global active_process

    await update.message.reply_text(
        "📦 Setup shuru!\n\n"
        "Ye install hoga:\n"
        "☕ OpenJDK 17\n"
        "📱 Android SDK\n"
        "🔧 Build Tools 31.0.0\n"
        "⚙️ NDK 21.0.6113669\n\n"
        "⏳ Ruko..."
    )

    commands = [
        ("☕ Java install...",
         "sudo apt update && sudo apt install -y openjdk-17-jdk wget unzip"),

        ("📁 SDK folder...",
         "mkdir -p ~/android-sdk/cmdline-tools"),

        ("⬇️ SDK download...",
         "wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O ~/android-sdk/cmdline-tools/tools.zip"),

        ("📂 SDK extract...",
         "cd ~/android-sdk/cmdline-tools && unzip -q tools.zip && mv cmdline-tools latest"),

        ("🔑 Licenses accept...",
         "yes | ~/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses"),

        ("📱 Platform 31...",
         "~/android-sdk/cmdline-tools/latest/bin/sdkmanager 'platforms;android-31'"),

        ("🔧 Build Tools...",
         "~/android-sdk/cmdline-tools/latest/bin/sdkmanager 'build-tools;31.0.0'"),

        ("⚙️ NDK...",
         "~/android-sdk/cmdline-tools/latest/bin/sdkmanager 'ndk;21.0.6113669'"),

        ("🐍 Python packages...",
         "pip install python-telegram-bot google-generativeai python-dotenv"),
    ]

    for msg, cmd in commands:
        if active_process == "STOPPED":
            await update.message.reply_text(
                "🛑 Setup rok diya!\n"
                "/codespace - Dobara shuru karo"
            )
            active_process = None
            return

        await update.message.reply_text(f"⏳ {msg}")

        process = await asyncio.create_subprocess_shell(
            cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )

        active_process = process
        stdout, stderr = await process.communicate()

        if process.returncode != 0:
            error = stderr.decode()
            await update.message.reply_text(
                f"❌ Error!\n\n"
                f"📌 Step: {msg}\n\n"
                f"🔴 Full Error:\n{error[:3000]}"
            )
            active_process = None
            return

        await update.message.reply_text(f"✅ Done: {msg}")

    active_process = None
    await update.message.reply_text(
        "🎉 Setup Complete!\n\n"
        "✔️ Java\n"
        "✔️ Android SDK\n"
        "✔️ Build Tools\n"
        "✔️ NDK\n\n"
        "🚀 Ab ZIP bhejo!"
    )

# ================================
# /stop
# ================================
async def stop_process(update, context):
    global active_process

    if active_process and active_process != "STOPPED":
        active_process = "STOPPED"
        try:
            active_process.kill()
        except:
            pass
        await update.message.reply_text(
            "🛑 Process rok diya!\n\n"
            "/start - Main menu"
        )
    else:
        await update.message.reply_text(
            "⚠️ Koi process nahi chal raha!\n"
            "/codespace - Setup shuru karo"
        )

# ================================
# GEMINI ERROR FIX
# ================================
async def fix_with_gemini(error_log, update):
    await update.message.reply_text("🤖 Gemini error analyse kar raha hai...")

    prompt = f"""
    Android Gradle build error aaya hai:
    {error_log}
    
    Sirf JSON do, kuch aur mat likho:
    {{
        "error_reason": "kyu aaya error",
        "file_to_fix": "file/path",
        "fixed_content": "pura fixed file content"
    }}
    """

    response = model.generate_content(prompt)
    return response.text

# ================================
# BUILD
# ================================
async def build_project(update, project_path):
    await update.message.reply_text("🔨 Build shuru...")
    os.chmod(f"{project_path}/gradlew", 0o755)

    process = await asyncio.create_subprocess_exec(
        "./gradlew", "assembleDebug",
        cwd=project_path,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE
    )

    output_lines = []
    while True:
        line = await process.stdout.readline()
        if not line:
            break
        line = line.decode().strip()
        output_lines.append(line)
        if line:
            await update.message.reply_text(f"📦 {line}")

    await process.wait()
    return process.returncode, "\n".join(output_lines)

# ================================
# AUTO FIX LOOP
# ================================
async def auto_fix_and_build(update, project_path, attempt=1):
    if attempt > 5:
        await update.message.reply_text(
            "❌ 5 attempts ke baad bhi fail!\n"
            "Manual check karo bro."
        )
        return

    await update.message.reply_text(f"🔄 Build Attempt #{attempt}/5")
    returncode, output = await build_project(update, project_path)

    if returncode == 0:
        apk_path = f"{project_path}/app/build/outputs/apk/debug/app-debug.apk"
        await update.message.reply_text("✅ Build Successful!")
        await update.message.reply_document(
            document=open(apk_path, 'rb'),
            filename="app-debug.apk",
            caption="🎉 APK ready! Install karo!"
        )
    else:
        await update.message.reply_text(
            f"❌ Attempt #{attempt} Failed!\n"
            "🤖 AI fix kar raha hai..."
        )

        fix_response = await fix_with_gemini(output, update)

        try:
            clean = fix_response.replace("```json","").replace("```","").strip()
            fix_data = json.loads(clean)

            file_path = f"{project_path}/{fix_data['file_to_fix']}"
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, 'w') as f:
                f.write(fix_data['fixed_content'])

            await update.message.reply_text(
                f"🔧 Fix Apply Hua!\n\n"
                f"📁 File: {fix_data['file_to_fix']}\n"
                f"💡 Reason: {fix_data['error_reason']}\n\n"
                f"🔄 Dobara build..."
            )

            await auto_fix_and_build(update, project_path, attempt + 1)

        except Exception as e:
            await update.message.reply_text(
                f"⚠️ AI parse error:\n{str(e)}\n\n"
                f"Raw response:\n{fix_response[:2000]}"
            )

# ================================
# ZIP HANDLER
# ================================
async def handle_zip(update, context):
    await update.message.reply_text("📥 ZIP mila! Process shuru...")

    file = await update.message.document.get_file()
    os.makedirs(BUILD_DIR, exist_ok=True)
    zip_path = f"{BUILD_DIR}/project.zip"
    await file.download_to_drive(zip_path)

    project_path = f"{BUILD_DIR}/project"
    if os.path.exists(project_path):
        shutil.rmtree(project_path)

    with zipfile.ZipFile(zip_path, 'r') as z:
        z.extractall(project_path)

    # File structure show
    files_list = ""
    for root, dirs, files in os.walk(project_path):
        level = root.replace(project_path, '').count(os.sep)
        indent = "  " * level
        files_list += f"📁 {indent}{os.path.basename(root)}/\n"
        for f in files:
            files_list += f"📄 {indent}  {f}\n"

    await update.message.reply_text(
        f"📂 Project Structure:\n\n{files_list[:3000]}"
    )

    await auto_fix_and_build(update, project_path)

# ================================
# MAIN
# ================================
def main():
    app = ApplicationBuilder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("codespace", setup_codespace))
    app.add_handler(CommandHandler("stop", stop_process))
    app.add_handler(MessageHandler(filters.Document.ZIP, handle_zip))
    print("✅ Bot chal raha hai...")
    app.run_polling()

if __name__ == "__main__":
    main()