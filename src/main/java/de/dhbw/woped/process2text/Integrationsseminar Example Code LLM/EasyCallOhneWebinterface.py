import requests
import time  # For measuring response time

# LM Studio API Endpoint
LM_STUDIO_URL = "http://localhost:1234/v1/chat/completions"

# Parameters for the API call
model_name = "llama-3.2-1b-instruct"
system_message = "You are a helpful assistant."
user_message = "What is the capital of France?"

# POST data for LM Studio
payload = {
    "model": model_name,
    "messages": [
        {"role": "system", "content": system_message},
        {"role": "user", "content": user_message}
    ],
    "temperature": 0.1,
    "max_tokens": -1,
    "stream": False
}

try:
    # Start measuring time
    start_time = time.time()

    # Send request to LM Studio
    response = requests.post(LM_STUDIO_URL, json=payload)
    response.raise_for_status()
    result = response.json()
    answer = result["choices"][0]["message"]["content"]

    # End measuring time
    end_time = time.time()

    # Calculate the response time
    response_time = end_time - start_time

    # Print the result to the console
    print(f"System Message: {system_message}")
    print(f"User Message: {user_message}")
    print(f"Answer: {answer}")
    print(f"Response Time: {response_time:.2f} seconds")

except Exception as e:
    print(f"An error occurred: {e}")
