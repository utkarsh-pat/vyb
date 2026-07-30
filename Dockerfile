FROM node:22-bookworm-slim

WORKDIR /app

RUN corepack enable

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/backend/package.json ./apps/backend/package.json
COPY packages/config/package.json ./packages/config/package.json

RUN pnpm install --frozen-lockfile --prod --filter @vyb/backend...

COPY apps/backend ./apps/backend
COPY packages/config ./packages/config
COPY packages/dataconnect/campus-admin-sdk ./packages/dataconnect/campus-admin-sdk
COPY packages/dataconnect/identity-admin-sdk ./packages/dataconnect/identity-admin-sdk
COPY packages/dataconnect/marketplace-admin-sdk ./packages/dataconnect/marketplace-admin-sdk
COPY packages/dataconnect/moderation-admin-sdk ./packages/dataconnect/moderation-admin-sdk
COPY packages/dataconnect/resources-admin-sdk ./packages/dataconnect/resources-admin-sdk
COPY packages/dataconnect/social-admin-sdk ./packages/dataconnect/social-admin-sdk

ENV NODE_ENV=production
ENV PORT=8080

EXPOSE 8080

CMD ["pnpm", "--filter", "@vyb/backend", "start"]
