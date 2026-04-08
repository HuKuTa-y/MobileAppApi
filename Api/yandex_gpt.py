import requests

# --- НАСТРОЙКИ ---
API_KEY = "AQVNycnST4heyRx3QdVPc8jGyLYX_f5I_XWC8sSg"  # Твой рабочий API-ключ
FOLDER_ID = "b1gih7j22o930q0sp06j"  # Вставь сюда свой реальный Folder ID (начинается с b1g)


def ask_yandex_gpt(prompt, system_prompt="Ты юрист-помощник. Отвечай кратко и по делу, ссылаясь на законы РФ."):
    url = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Api-Key {API_KEY}",
        "x-folder-id": FOLDER_ID
    }

    payload = {
        "modelUri": f"gpt://{FOLDER_ID}/yandexgpt/latest",
        "completionOptions": {
            "temperature": 0.3,
            "maxTokens": 1000
        },
        "messages": [
            {"role": "system", "text": system_prompt},
            {"role": "user", "text": prompt}
        ]
    }

    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)

        if response.status_code == 200:
            result = response.json()
            return result["result"]["alternatives"][0]["message"]["text"]
        else:
            return f"Ошибка API ({response.status_code}): {response.text}"

    except Exception as e:
        return f"Ошибка соединения: {str(e)}"


if __name__ == "__main__":
    print("🤖 Тест чата...")
    answer = ask_yandex_gpt("Меня уволили без причины, что делать?")
    print("\nОтвет нейросети:")
    print(answer)