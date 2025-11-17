#!/bin/bash
BASE_URL="http://localhost:8080/api"
APP="myApp"
USER="test-user"

echo "1. Creating session..."
curl -s -X POST "$BASE_URL/sessions/create?appName=$APP&userId=$USER&sessionId=session-001" | jq

echo -e "\n2. Adding event..."
curl -s -X POST "$BASE_URL/sessions/session-001/events?appName=$APP&userId=$USER" \
-H "Content-Type: application/json" \
-d '{"id":"evt-001","author":"user","content":{"parts":[{"text":"Hello from ADK testing"}],"role":"user"},"timestamp':1731767281000,"partial":false,"turnComplete":true}' | jq

echo -e "\n3. Getting session..."
curl -s -X GET "$BASE_URL/sessions/session-001?appName=$APP&userId=$USER" | jq

echo -e "\n4. Adding to memory..."
curl -s -X POST "$BASE_URL/memory/add/session-001" \
-H "Content-Type: application/json" \
-d '{"id":"session-001","appName":"myApp","userId":"test-user","state":{},"events":[{"id":"evt-001","author":"user","content":{"parts":[{"text":"Hello from ADK testing"}],"role":"user"},"timestamp":1731767281000}]}' | jq

echo -e "\n5. Searching memory..."
curl -s -X GET "$BASE_URL/memory/search?appName=$APP&userId=$USER&query=ADK" | jq

echo -e "\n6. Deleting session..."
curl -s -X DELETE "$BASE_URL/sessions/session-001?appName=$APP&userId=$USER" | jq