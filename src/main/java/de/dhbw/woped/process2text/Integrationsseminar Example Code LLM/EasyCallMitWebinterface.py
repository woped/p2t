from flask import Flask, request, jsonify, render_template
import requests
import time  # For measuring response time

app = Flask(__name__)

# LM Studio API Endpoint
LM_STUDIO_URL = "http://localhost:1234/v1/chat/completions"

# Route for the main page
@app.route('/')
def index():
    return render_template('index.html')

# API endpoint to send the request to LM Studio
@app.route('/send', methods=['POST'])
def send_to_model():
    # Get data from the browser
    user_message = request.form.get('message')
    system_message = request.form.get('system_message')

    # Model name (used for the request payload)
    model_name = "llama-3.2-1b-instruct"

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

        # Return the response and time as JSON
        return jsonify({"response": answer, "response_time": response_time})
    except Exception as e:
        return jsonify({"error": str(e)})

if __name__ == '__main__':
    app.run(debug=True, port=5000)
