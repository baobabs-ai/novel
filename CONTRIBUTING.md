# Contributing Code

Thank you for your interest in contributing to this project! To collaborate effectively, please follow these guidelines.

- Before writing code, please discuss your planned changes via an Issue or in the group to ensure they align with the current development direction.
- When submitting a Pull Request, please keep it concise and focused on a single change to facilitate quick review and merging.
- If you have questions about the current code design, you can ask @FishHawk in the group.
- If you use AI to assist with writing, please be sure to review the code yourself.

## How to Contribute to Frontend Development

The website is developed based on Vue3 + TypeScript + Vite + [Naive ui](https://www.naiveui.com/zh-CN).

First, prepare the development environment:

```bash
git clone git@github.com:auto-novel/auto-novel.git
cd web
pnpm install --frozen-lockfile # Install dependencies
pnpm prepare                   # Set up Git hooks
```

Then, according to your needs, choose the appropriate way to start the development server:

```bash
pnpm dev        # Start the development server, connecting to the production environment backend server of the machine translation site
pnpm dev:local  # Start the development server, connecting to a locally started backend server at http://localhost:8081
pnpm dev --host # Start the development server, connecting to the production environment backend server of the machine translation site, and allowing LAN access for debugging on mobile phones
```

Note: If the development server is connected to the **production** backend of the machine translation site, please avoid polluting the website's database during development. For security reasons, requests to upload chapter translations are blocked in the development environment.

## How to Contribute to Backend Development

The backend is developed based on JVM17 + Kotlin + Ktor. It is recommended to open the project with IntelliJ IDEA.

If your changes involve the database, you need to [deploy the database](https://github.com/auto-novel/auto-novel/blob/main/README.md#部署) yourself and set the environment variables:

```bash
DB_HOST_TEST=127.0.0.1 # Database IP address
```

If your changes do not involve the Http API, you can use kotest to write unit tests for debugging. It is recommended to install the kotest plugin.

If your changes involve the Http API, you can use `pnpm dev:local` to start the development server. Refer to the "How to Contribute to Frontend Development" section.

> [!NOTE]
> NixOS development environment configuration can be found at [flake.nix](https://gist.github.com/kurikomoe/9dd60f9613e0b8f75c137779d223da4f). Since devenv is used, `--impure` is required.
