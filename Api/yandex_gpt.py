# yandex_gpt.py
import requests
import os
from datetime import datetime, timedelta

# 
YANDEX_API_KEY = os.getenv("YANDEX_API_KEY")
FOLDER_ID = os.getenv("YANDEX_FOLDER_ID")   

if not YANDEX_API_KEY or not FOLDER_ID:
    raise RuntimeError("YANDEX_API_KEY и YANDEX_FOLDER_ID должны быть заданы в переменных окружения")

# Кэш для IAM токена (действует 1 час)
iam_token_cache = {
    "token": None,
    "expires": None
}

def get_iam_token():
    """Получает IAM токен (кэширует на 1 час)"""
    now = datetime.now()
    
    # Если токен ещё действителен — возвращаем из кэша
    if iam_token_cache["token"] and iam_token_cache["expires"] > now:
        return iam_token_cache["token"]
    
    # Запрашиваем новый токен
    response = requests.post(
        "https://iam.api.cloud.yandex.net/iam/v1/tokens",
        headers={"Authorization": f"Api-Key {YANDEX_API_KEY}"}
    )
    
    if response.status_code != 200:
        raise Exception(f"Failed to get IAM token: {response.text}")
    
    token = response.json()["iamToken"]
    
    # Кэшируем на 55 минут (с запасом)
    iam_token_cache["token"] = token
    iam_token_cache["expires"] = now + timedelta(minutes=55)
    
    return token

def ask_yandex_gpt(prompt, system_prompt="Ты юрист-помощник. Отвечай по законам РФ."):
    """Отправляет запрос к YandexGPT"""
    iam_token = get_iam_token()
    
    response = requests.post(
        "https://llm.api.cloud.yandex.net/foundationModels/v1/completion",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {iam_token}",
            "x-folder-id": FOLDER_ID,
        },
        json={
            "modelUri": f"gpt://{FOLDER_ID}/yandexgpt/latest",
            "completionOptions": {
                "stream": False,
                "temperature": 0.3,
                "maxTokens": 1000,
            },
            "messages": [
                {"role": "system", "text": system_prompt},
                {"role": "user", "text": prompt},
            ],
        },
    )
    
    if response.status_code != 200:
        raise Exception(f"YandexGPT error: {response.text}")
    
    result = response.json()["result"]["alternatives"][0]["message"]["text"]
    return result

# ТЕСТ ПРИ ЗАПУСКЕ
if __name__ == "__main__":
    print("Тестируем YandexGPT...")
    answer = ask_yandex_gpt("Меня уволили без причины, что делать?")
    print("Ответ:", answer)