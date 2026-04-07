# Second Brain 

## 1. What is second brain
So, Second brain is a software that unifies all notes from notion and docs in one platform. It shows all the notes in form of a graph With relation being the edges and nodes being the notes, to identify similar context.
It also provides AI bot for grapg traversal and note discovery

## 1. How did you implement the graph?
So, on frontend I used d3.js It's a js library for visualising graphs. It connects to backend by calling fetch api 

I could have used axios as well for making http requests, But I am using fetch.
https://medium.com/@johnnyJK/axios-vs-fetch-api-selecting-the-right-tool-for-http-requests-ecb14e39e285

API's are called like this in frontend
const response = await fetch(apiUrl('/api/graph/feed')
- /api/graph/feed
- /api/me
- /api/notion/ingestRaw
- /api/google-docs/doc/ or /api/notion/page/
- /api/graph/ask

For backend, I used springboot to do this
- 
- 



