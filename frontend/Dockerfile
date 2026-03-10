FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=
ARG VITE_NOTION_CLIENT_ID=
ARG VITE_NOTION_REDIRECT_URI=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_NOTION_CLIENT_ID=$VITE_NOTION_CLIENT_ID
ENV VITE_NOTION_REDIRECT_URI=$VITE_NOTION_REDIRECT_URI
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
