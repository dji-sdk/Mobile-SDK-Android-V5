FROM node:24-alpine

WORKDIR /app

COPY package.json server.mjs store.mjs seed.mjs ./
COPY data ./data
COPY web ./web

ENV HOST=0.0.0.0 \
    PORT=18081 \
    DATA_DIR=/app/runtime

RUN mkdir -p /app/runtime && chown -R node:node /app

USER node

EXPOSE 18081

CMD ["node", "server.mjs"]
